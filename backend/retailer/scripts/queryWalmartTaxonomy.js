const https = require("https");
const { createWalmartAuthHeaders } = require("../src/walmartSignature");

const TAXONOMY_URL =
  "https://developer.api.walmart.com/api-proxy/service/affil/product/v2/taxonomy";

if (!process.env.WALMART_CONSUMER_ID) {
  console.error("Missing WALMART_CONSUMER_ID.");
  console.error(
    "Run: $env:WALMART_CONSUMER_ID='your-consumer-id'; node scripts/queryWalmartTaxonomy.js"
  );
  process.exit(1);
}

if (!process.env.WALMART_KEY_VERSION) {
  console.error("Missing WALMART_KEY_VERSION.");
  console.error("Run: $env:WALMART_KEY_VERSION='1'");
  process.exit(1);
}

const headers = createWalmartAuthHeaders();

const request = https.get(TAXONOMY_URL, { headers }, (response) => {
  let body = "";

  response.setEncoding("utf8");
  response.on("data", (chunk) => {
    body += chunk;
  });
  response.on("end", () => {
    console.log(`Status: ${response.statusCode}`);

    try {
      console.log(JSON.stringify(JSON.parse(body), null, 2));
    } catch {
      console.log(body);
    }

    if (response.statusCode < 200 || response.statusCode >= 300) {
      process.exitCode = 1;
    }
  });
});

request.on("error", (error) => {
  console.error(error.message);
  process.exit(1);
});
