const crypto = require("crypto");
const fs = require("fs");
const os = require("os");
const path = require("path");

function resolvePrivateKeyPath() {
  return process.env.WALMART_PRIVATE_KEY_PATH ||
    path.join(os.homedir(), ".walmart-api-keys", "walmart_private_key_pkcs8.pem");
}

function hasWalmartCredentials() {
  const privateKeyPath = resolvePrivateKeyPath();
  return Boolean(
    process.env.WALMART_CONSUMER_ID &&
    process.env.WALMART_KEY_VERSION &&
    (process.env.WALMART_PRIVATE_KEY_PEM || fs.existsSync(privateKeyPath)),
  );
}

function readPrivateKey() {
  if (process.env.WALMART_PRIVATE_KEY_PEM) {
    return process.env.WALMART_PRIVATE_KEY_PEM.replace(/\\n/g, "\n");
  }

  const privateKeyPath = resolvePrivateKeyPath();
  if (!fs.existsSync(privateKeyPath)) {
    throw new Error(`Walmart private key file not found: ${privateKeyPath}`);
  }
  return fs.readFileSync(privateKeyPath, "utf8");
}

function createWalmartAuthHeaders() {
  const consumerId = process.env.WALMART_CONSUMER_ID;
  const keyVersion = process.env.WALMART_KEY_VERSION || "1";

  if (!consumerId) {
    throw new Error("Missing WALMART_CONSUMER_ID");
  }

  const timestamp = Date.now().toString();
  const privateKey = readPrivateKey();
  const stringToSign = `${consumerId}\n${timestamp}\n${keyVersion}\n`;
  const signer = crypto.createSign("RSA-SHA256");
  signer.update(stringToSign, "utf8");
  const signature = signer.sign(privateKey);

  return {
    Accept: "application/json",
    "WM_CONSUMER.ID": consumerId,
    "WM_CONSUMER.INTIMESTAMP": timestamp,
    "WM_SEC.KEY_VERSION": keyVersion,
    "WM_SEC.AUTH_SIGNATURE": signature.toString("base64"),
  };
}

module.exports = {
  createWalmartAuthHeaders,
  hasWalmartCredentials,
};
