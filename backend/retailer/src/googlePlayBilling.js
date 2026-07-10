const crypto = require("crypto");

const DEFAULT_APP_TOKEN = "R7qK2mZ9vP4xT0aLN6cY1sD8wF3hJ5bG";
const DEFAULT_PACKAGE_NAME = "com.ciblorenzo.whatsonmyfood";
const DEFAULT_PRODUCT_ID = "bitwise_plus_monthly";
const ANDROID_PUBLISHER_SCOPE = "https://www.googleapis.com/auth/androidpublisher";
const TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
const MAX_BODY_BYTES = 64 * 1024;

let cachedAccessToken = "";
let cachedAccessTokenExpiryMs = 0;

function base64Url(value) {
  return Buffer.from(value)
    .toString("base64")
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");
}

function serviceAccountCredentials() {
  let parsed = null;
  const encoded = process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON;
  if (encoded) {
    try {
      parsed = JSON.parse(encoded);
    } catch (_plainJsonError) {
      try {
        parsed = JSON.parse(Buffer.from(encoded, "base64").toString("utf8"));
      } catch (_base64JsonError) {
        throw new Error("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON is not valid JSON or base64 JSON");
      }
    }
  }

  const clientEmail = parsed?.client_email || process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL || "";
  const privateKey = (parsed?.private_key || process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_PRIVATE_KEY || "")
    .replace(/\\n/g, "\n");
  if (!clientEmail || !privateKey) return null;
  return { clientEmail, privateKey };
}

function signedServiceAccountAssertion(credentials, nowSeconds) {
  const header = base64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const payload = base64Url(JSON.stringify({
    iss: credentials.clientEmail,
    scope: ANDROID_PUBLISHER_SCOPE,
    aud: TOKEN_ENDPOINT,
    iat: nowSeconds,
    exp: nowSeconds + 3600,
  }));
  const unsigned = `${header}.${payload}`;
  const signature = crypto.sign("RSA-SHA256", Buffer.from(unsigned), credentials.privateKey);
  return `${unsigned}.${base64Url(signature)}`;
}

async function accessToken() {
  if (cachedAccessToken && Date.now() < cachedAccessTokenExpiryMs - 60_000) {
    return cachedAccessToken;
  }
  const credentials = serviceAccountCredentials();
  if (!credentials) throw new Error("Google Play service account is not configured");

  const assertion = signedServiceAccountAssertion(credentials, Math.floor(Date.now() / 1000));
  const response = await fetch(TOKEN_ENDPOINT, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  const responseText = await response.text();
  if (!response.ok) throw new Error(`Google OAuth returned ${response.status}: ${responseText.slice(0, 300)}`);
  const token = JSON.parse(responseText);
  cachedAccessToken = token.access_token;
  cachedAccessTokenExpiryMs = Date.now() + Number(token.expires_in || 3600) * 1000;
  return cachedAccessToken;
}

function latestExpiryTime(lineItems) {
  return (lineItems || [])
    .map((item) => item.expiryTime || "")
    .filter(Boolean)
    .sort()
    .at(-1) || "";
}

function evaluateSubscription(subscription, expectedProductId, expectedAccountIdHash, nowMs = Date.now()) {
  const lineItems = Array.isArray(subscription?.lineItems) ? subscription.lineItems : [];
  const productMatches = lineItems.some((item) => item.productId === expectedProductId);
  const remoteAccountId = subscription?.externalAccountIdentifiers?.obfuscatedExternalAccountId || "";
  const accountMatches = Boolean(expectedAccountIdHash) && remoteAccountId === expectedAccountIdHash;
  const state = subscription?.subscriptionState || "SUBSCRIPTION_STATE_UNSPECIFIED";
  const expiryTime = latestExpiryTime(lineItems);
  const notExpired = expiryTime ? Date.parse(expiryTime) > nowMs : false;
  const entitledState = new Set([
    "SUBSCRIPTION_STATE_ACTIVE",
    "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
    "SUBSCRIPTION_STATE_CANCELED",
  ]).has(state);

  return {
    verified: productMatches && accountMatches,
    entitlementActive: productMatches && accountMatches && entitledState && notExpired,
    pending: state === "SUBSCRIPTION_STATE_PENDING",
    state,
    expiryTime,
    acknowledgementPending: subscription?.acknowledgementState === "ACKNOWLEDGEMENT_STATE_PENDING",
  };
}

async function acknowledgeSubscription(packageName, productId, purchaseToken, token) {
  const endpoint = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/"
    + `${encodeURIComponent(packageName)}/purchases/subscriptions/${encodeURIComponent(productId)}`
    + `/tokens/${encodeURIComponent(purchaseToken)}:acknowledge`;
  const response = await fetch(endpoint, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ developerPayload: "Bitwise Plus entitlement granted" }),
  });
  if (!response.ok && response.status !== 409) {
    const responseText = await response.text();
    throw new Error(`Google Play acknowledgement returned ${response.status}: ${responseText.slice(0, 300)}`);
  }
}

async function verifySubscription(purchaseToken, accountIdHash) {
  const packageName = process.env.GOOGLE_PLAY_PACKAGE_NAME || DEFAULT_PACKAGE_NAME;
  const productId = process.env.GOOGLE_PLAY_SUBSCRIPTION_PRODUCT_ID || DEFAULT_PRODUCT_ID;
  const token = await accessToken();
  const endpoint = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/"
    + `${encodeURIComponent(packageName)}/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`;
  const response = await fetch(endpoint, { headers: { Authorization: `Bearer ${token}` } });
  const responseText = await response.text();
  if (!response.ok) throw new Error(`Google Play verification returned ${response.status}: ${responseText.slice(0, 300)}`);
  const subscription = JSON.parse(responseText);
  const decision = evaluateSubscription(subscription, productId, accountIdHash);
  if (decision.entitlementActive && decision.acknowledgementPending) {
    try {
      await acknowledgeSubscription(packageName, productId, purchaseToken, token);
      decision.acknowledgementPending = false;
    } catch (error) {
      console.error("Google Play acknowledgement failed:", error.message);
    }
  }
  return decision;
}

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    let body = "";
    let size = 0;
    req.setEncoding("utf8");
    req.on("data", (chunk) => {
      size += Buffer.byteLength(chunk);
      if (size > MAX_BODY_BYTES) {
        reject(new Error("Billing request is too large"));
        req.destroy();
        return;
      }
      body += chunk;
    });
    req.on("end", () => {
      try {
        resolve(JSON.parse(body || "{}"));
      } catch (_error) {
        reject(new Error("Billing request must contain valid JSON"));
      }
    });
    req.on("error", reject);
  });
}

async function handleGooglePlayVerification(req) {
  const expectedToken = process.env.BITWISE_APP_TOKEN || DEFAULT_APP_TOKEN;
  if (expectedToken && req.headers["x-app-token"] !== expectedToken) {
    return { status: 401, body: { error: "Unauthorized" } };
  }
  if (!serviceAccountCredentials()) {
    return { status: 503, body: { error: "Google Play verification is not configured" } };
  }

  const body = await readJsonBody(req);
  const purchaseToken = typeof body.purchaseToken === "string" ? body.purchaseToken.trim() : "";
  const accountIdHash = typeof body.accountIdHash === "string" ? body.accountIdHash.trim().toLowerCase() : "";
  if (!purchaseToken || purchaseToken.length > 4096 || !/^[a-f0-9]{64}$/.test(accountIdHash)) {
    return { status: 400, body: { error: "A valid purchase token and account identifier are required" } };
  }

  try {
    return { status: 200, body: await verifySubscription(purchaseToken, accountIdHash) };
  } catch (error) {
    console.error("Google Play verification failed:", error.message);
    return { status: 502, body: { error: "Google Play could not verify this purchase" } };
  }
}

function resetTokenCacheForTests() {
  cachedAccessToken = "";
  cachedAccessTokenExpiryMs = 0;
}

module.exports = {
  evaluateSubscription,
  handleGooglePlayVerification,
  resetTokenCacheForTests,
  signedServiceAccountAssertion,
  verifySubscription,
};
