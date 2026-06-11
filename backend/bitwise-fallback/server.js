const http = require("http");

const port = Number(process.env.PORT || 8000);
const APP_TOKEN = process.env.BITWISE_APP_TOKEN || "R7qK2mZ9vP4xT0aLN6cY1sD8wF3hJ5bG";

function writeJson(res, status, body) {
  res.writeHead(status, {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": "*",
  });
  res.end(JSON.stringify(body));
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let body = "";
    req.setEncoding("utf8");
    req.on("data", (chunk) => {
      body += chunk;
    });
    req.on("end", () => resolve(body));
    req.on("error", reject);
  });
}

function contentText(messageContent) {
  if (typeof messageContent === "string") return messageContent;
  if (Array.isArray(messageContent)) {
    return messageContent
      .map((part) => part && part.type === "text" ? part.text : "")
      .filter(Boolean)
      .join("\n");
  }
  return "";
}

function extractAfter(text, label) {
  const match = text.match(new RegExp(label + "\\s*:?\\s*([^\\n]+)", "i"));
  return match ? match[1].trim() : "";
}

function productEvidence(prompt) {
  const ocrMatch = prompt.match(/OCR TEXT:\s*([\s\S]*?)(?:\n\s*Return valid JSON only\.|$)/i);
  if (ocrMatch && ocrMatch[1].trim()) return ocrMatch[1].trim();
  return prompt;
}

function extractIngredients(text) {
  const source = extractAfter(text, "Ingredients") || extractAfter(text, "ingredients_text") || "";
  if (!source) return [];
  return source
    .split(/,(?![^()]*\))/)
    .map((item) => item.replace(/[_*]/g, "").trim())
    .filter(Boolean)
    .slice(0, 30);
}

function ingredientDefinition(prompt) {
  const ingredient = (prompt.match(/ingredient or additive:\s*([^\n]+)/i) || [])[1] || "";
  const name = ingredient.trim();
  return {
    name,
    category: name ? "Food ingredient" : "",
    function: name ? "Used as part of the product formulation." : "",
    explanation: name
      ? "This fallback Bitwise service can provide a basic definition, but detailed safety context needs the full AI service."
      : "",
    health_status: name ? "MODERATE" : "",
    source_name: "FDA Food Additives and GRAS Ingredients",
    source_url: "https://www.fda.gov/food/food-ingredients-packaging/food-additives-and-gras-ingredients-information-consumers",
  };
}

function productAnalysis(prompt) {
  const evidence = productEvidence(prompt);
  const extractedName = extractAfter(evidence, "Product") || extractAfter(evidence, "product_name") || "";
  const productName = isPlaceholderProductName(extractedName) ? "" : extractedName;
  const brand = extractAfter(evidence, "Brand") || extractAfter(evidence, "brands") || "";
  const ingredients = extractIngredients(evidence);
  const searchable = ingredients.length > 0 ? ingredients.join(" ") : evidence;
  const lower = searchable.toLowerCase();
  const warningTerms = [
    "red 40",
    "yellow 5",
    "blue 1",
    "high fructose corn syrup",
    "partially hydrogenated",
    "fully hydrogenated",
    "hydrogenated vegetable oil",
    "aspartame",
    "sucralose",
    "sodium nitrite",
  ];
  const refinedOilTerms = [
    "canola oil",
    "sunflower oil",
    "soybean oil",
    "corn oil",
    "vegetable oil",
    "palm oil",
    "palm kernel oil",
    "cottonseed oil",
    "safflower oil",
  ];
  const warnings = warningTerms.filter((term) => lower.indexOf(term) !== -1);
  const refinedOils = refinedOilTerms.filter((term) => lower.indexOf(term) !== -1);
  const hasEvidence = ingredients.length > 0 || evidence.length > 20;
  const reason = warnings.length > 0
    ? "Contains ingredients that commonly deserve a closer look."
    : hasEvidence
      ? "No major high-concern ingredient was detected in the available label text."
      : "The full Bitwise AI service is offline, and there was not enough label text for a reliable review.";

  return {
    product_name: productName,
    brand,
    product_type: lower.indexOf("water") !== -1 ? "beverage" : "food",
    verdict: warnings.length > 0 ? "NOT_HEALTHY" : "REVIEW",
    verdict_reason: reason,
    ingredients,
    summary: naturalProductSummary(productName, brand, lower, ingredients, warnings, refinedOils, hasEvidence),
    findings: warnings.length > 0
      ? warnings.map((term) => ({
          rule: "Ingredient watch item",
          impact: "warning",
          triggering_ingredient: term,
          explanation: "This ingredient is worth noticing because many shoppers choose to limit it in everyday packaged foods.",
          source_url: "https://www.fda.gov/food/food-ingredients-packaging/food-additives-and-gras-ingredients-information-consumers",
        }))
      : refinedOils.length > 0
        ? refinedOils.map((term) => ({
            rule: "Refined oil watch item",
            impact: "warning",
            triggering_ingredient: term,
            explanation: "This is a refined seed or vegetable oil. It is not the same as partially hydrogenated oil unless the label explicitly says hydrogenated or partially hydrogenated.",
            source_url: "https://www.fda.gov/food/food-additives-petitions/trans-fat",
          }))
        : [{
          rule: "Ingredient review",
          impact: "neutral",
          triggering_ingredient: "",
          explanation: "The available ingredient list reads as low concern for everyday label review.",
          source_url: "https://www.fda.gov/food/food-ingredients-packaging/food-additives-and-gras-ingredients-information-consumers",
        }],
    sources: [{
      name: warnings.length > 0 ? "FDA Food Additives and GRAS Ingredients" : refinedOils.length > 0 ? "FDA Trans Fat" : "WHO Healthy Diet",
      url: warnings.length > 0
        ? "https://www.fda.gov/food/food-ingredients-packaging/food-additives-and-gras-ingredients-information-consumers"
        : refinedOils.length > 0
          ? "https://www.fda.gov/food/food-additives-petitions/trans-fat"
        : "https://www.who.int/news-room/fact-sheets/detail/healthy-diet",
      visual_quote: "",
      search_query: warnings.length > 0 ? "food additive safety" : refinedOils.length > 0 ? "partially hydrogenated oil trans fat" : "healthy diet packaged foods",
    }],
  };
}

function naturalProductSummary(productName, brand, searchableText, ingredients, warnings, refinedOils, hasEvidence) {
  const displayName = [brand, productName]
    .filter((value) => value && !isPlaceholderProductName(value))
    .join(" ")
    .trim() || "This product";
  const isWaterLike = /\b(water|hydration|electrolyte)\b/i.test(searchableText);
  const isCulturedDairy = /\b(yogurt|yoghurt|kefir|cultured|milk|lactobacillus|bifidus|streptococcus thermophilus)\b/i.test(searchableText);
  const hasIngredients = ingredients.length > 0;

  if (!hasEvidence) {
    return "I need the ingredient panel before making a confident call on "
      + escapeHtml(displayName)
      + ". A front label or product name alone is not enough to judge additives, sweeteners, or processing."
      + "<br><br><b>Bottom line</b><br>Scan the back label when possible and treat this as incomplete until the ingredient list is visible.";
  }

  if (warnings.length > 0) {
    return escapeHtml(displayName)
      + " is not a great everyday pick from this label. The ingredient list includes "
      + escapeHtml(warnings.join(", "))
      + ", which is worth noticing even if one serving is not automatically dangerous. Serving size and frequency still matter, especially for sweet drinks and snack foods."
      + "<br><br><b>Bottom line</b><br>Keep it occasional, and compare it with a similar product that uses fewer sweeteners, colors, or preservatives.";
  }

  if (refinedOils.length > 0) {
    const ingredientPreview = ingredients.slice(0, 3).join(", ");
    return escapeHtml(displayName)
      + " has a decent start with "
      + escapeHtml(ingredientPreview || "its first ingredients")
      + ", but "
      + escapeHtml(refinedOils.join(", "))
      + " is worth calling out. Canola oil is a refined seed oil; it is not automatically hydrogenated, and I would only call it hydrogenated if the label used that word."
      + "<br><br><b>Bottom line</b><br>For a snack, keep an eye on refined oils alongside sodium, refined starches, and portion size.";
  }

  if (isWaterLike) {
    return escapeHtml(displayName)
      + " looks like a low-concern drink from the available label details. Enhanced waters can still carry sodium or flavor additives, so the serving size matters if you drink several in a day."
      + "<br><br><b>Bottom line</b><br>This is a reasonable everyday choice if the nutrition panel confirms low sugar and moderate sodium.";
  }

  if (isCulturedDairy && hasIngredients) {
    return escapeHtml(displayName)
      + " looks like a straightforward cultured dairy product. Milk plus live cultures is a normal yogurt-style ingredient pattern, and the cultures are there to ferment the milk rather than act as additives."
      + " The ingredient list is reassuring, but flavored versions can still be high in added sugar."
      + "<br><br><b>Bottom line</b><br>This is a solid choice if sugar is low; pair it with fruit or nuts if you want more fiber and staying power.";
  }

  if (hasIngredients) {
    const ingredientPreview = ingredients.slice(0, 3).join(", ");
    return escapeHtml(displayName)
      + " starts with "
      + escapeHtml(ingredientPreview)
      + ". The nutrition panel still decides the bigger picture, especially sugar, sodium, and serving size."
      + "<br><br><b>Bottom line</b><br>This can fit regularly if the nutrition facts match your goals.";
  }

  return escapeHtml(displayName)
    + " does not show an obvious red flag, but the ingredient panel is incomplete."
    + " The missing details matter most for sweeteners, colors, preservatives, and oils."
    + "<br><br><b>Bottom line</b><br>Use this as a cautious pass and rescan the ingredient panel for a stronger answer.";
}

function isPlaceholderProductName(value) {
  const normalized = String(value || "").toLowerCase().replace(/[^a-z0-9]+/g, " ").trim();
  return normalized === "scanned product"
    || normalized === "product name unavailable"
    || normalized === "unknown product"
    || normalized === "unknown"
    || normalized === "name"
    || normalized === "product";
}

function escapeHtml(value) {
  return String(value || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function completion(content) {
  return {
    id: "bitwise-fallback",
    object: "chat.completion",
    choices: [{
      index: 0,
      message: {
        role: "assistant",
        content: JSON.stringify(content),
      },
      finish_reason: "stop",
    }],
  };
}

async function handleBitwiseCompletion(req, writeJson) {
  if (APP_TOKEN && req.headers["x-app-token"] !== APP_TOKEN) {
    return { status: 401, body: { error: "Unauthorized" } };
  }

  const root = JSON.parse(await readBody(req));
  const messages = Array.isArray(root.messages) ? root.messages : [];
  const prompt = messages.map((message) => contentText(message.content)).join("\n");
  const result = /define the following food ingredient/i.test(prompt)
    ? ingredientDefinition(prompt)
    : productAnalysis(prompt);
  return { status: 200, body: completion(result) };
}

module.exports = {
  handleBitwiseCompletion,
};

if (require.main === module) {
  http.createServer(async (req, res) => {
    if (req.method === "GET" && req.url === "/health") {
      writeJson(res, 200, { ok: true, service: "bitwise-fallback" });
      return;
    }

    if (req.method !== "POST" || req.url !== "/v1/chat/completions") {
      writeJson(res, 404, { error: "Not found" });
      return;
    }

    try {
      const result = await handleBitwiseCompletion(req, writeJson);
      writeJson(res, result.status, result.body);
    } catch (error) {
      writeJson(res, 500, { error: error.message || "Bitwise fallback failed" });
    }
  }).listen(port, () => {
    console.log(`Bitwise fallback listening on http://localhost:${port}`);
  });
}
