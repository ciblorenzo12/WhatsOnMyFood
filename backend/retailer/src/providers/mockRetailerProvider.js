class MockRetailerProvider {
  constructor() {
    this.name = "MockRetailerProvider";
  }

  async getAvailability(query) {
    const productName = query.productName || "Scanned product";
    const brand = query.brand || "Brand";
    const encodedSearch = encodeURIComponent(`${brand} ${productName}`);

    return [
      {
        retailerName: "Brand Website",
        providerName: this.name,
        availabilityStatus: "Available online",
        price: "See site",
        distance: "Ships direct",
        fulfillment: "Delivery",
        productUrl: `https://www.google.com/search?q=${encodedSearch}+official+store`,
        note: "Shop from the brand or official online store.",
        available: true,
      },
      {
        retailerName: "Walmart",
        providerName: this.name,
        availabilityStatus: "Check store",
        price: "See Walmart",
        distance: "Nearby stores",
        fulfillment: "Pickup or delivery",
        productUrl: `https://www.walmart.com/search?q=${encodedSearch}`,
        note: "Open Walmart to confirm this product at the selected store.",
        available: true,
        priceValue: 0.0,
        distanceValue: 15.0,
      },
      {
        retailerName: "Target",
        providerName: this.name,
        availabilityStatus: "Search available",
        price: "Varies",
        distance: "3.1 mi",
        fulfillment: "Pickup",
        productUrl: `https://www.target.com/s?searchTerm=${encodedSearch}`,
        note: "Open Target to confirm this product at the selected store.",
        available: true,
      },
      {
        retailerName: "Publix",
        providerName: this.name,
        availabilityStatus: "Check store",
        price: "See Publix",
        distance: "Nearby stores",
        fulfillment: "Pickup or delivery",
        productUrl: `https://www.publix.com/search?searchTerm=${encodedSearch}`,
        note: "Open Publix to confirm this product at the selected store.",
        available: true,
        priceValue: 0.0,
        distanceValue: 15.0,
      },
      {
        retailerName: "Amazon",
        providerName: this.name,
        availabilityStatus: "Online marketplace",
        price: "Varies",
        distance: "Delivery",
        fulfillment: "Shipping",
        productUrl: `https://www.amazon.com/s?k=${encodedSearch}`,
        note: "Online option when nearby shelves do not show a match.",
        available: true,
      },
      {
        retailerName: "Kroger",
        providerName: this.name,
        availabilityStatus: "Search available",
        price: "Unavailable",
        distance: "Nearby stores",
        fulfillment: "Pickup",
        productUrl: `https://www.kroger.com/search?query=${encodedSearch}`,
        note: "Search Kroger for current pickup options.",
        available: false,
      },
      {
        retailerName: "Instacart",
        providerName: this.name,
        availabilityStatus: "Delivery search",
        price: "Unavailable",
        distance: "Local retailers",
        fulfillment: "Delivery",
        productUrl: `https://www.instacart.com/store/s?k=${encodedSearch}`,
        note: "Search nearby delivery options.",
        available: false,
      },
    ];
  }

  async getProduct() {
    return null;
  }

  async getAlternatives(query) {
    const productContext = this.normalize(`${query.category || ""} ${query.productName || ""}`);

    if (this.containsAny(productContext, ["soda", "sodas", "cola", "soft drink", "soft drinks"])) {
      return this.sodaAlternatives();
    }
    if (this.containsAny(productContext, [
      "protein bar",
      "protein bars",
      "energy bar",
      "energy bars",
      "nutrition bar",
      "nutrition bars",
      "snack bar",
      "snack bars",
      "granola bar",
      "granola bars",
    ])) {
      return this.barAlternatives();
    }
    if (this.containsAny(productContext, ["cereal", "cereals", "breakfast cereal", "breakfast cereals"])) {
      return this.cerealAlternatives();
    }
    if (this.containsAny(productContext, ["granola", "muesli"])) {
      return this.granolaAlternatives();
    }
    if (this.containsAny(productContext, ["yogurt", "yogurts", "yoghurt", "yoghurts", "skyr"])) {
      return this.yogurtAlternatives();
    }
    if (this.containsAny(productContext, ["chocolate", "chocolates", "candy", "candies", "sweets"])) {
      return this.chocolateAlternatives();
    }
    if (this.containsAny(productContext, ["cookie", "cookies", "biscuit", "biscuits"])) {
      return this.cookieAlternatives();
    }
    if (this.containsAny(productContext, [
      "chip",
      "chips",
      "crisps",
      "cracker",
      "crackers",
      "popcorn",
      "pretzel",
      "pretzels",
      "puffs",
      "tortilla chips",
      "potato chips",
    ])) {
      return this.snackAlternatives();
    }
    if (this.containsAny(productContext, ["bread", "breads", "bagel", "bagels", "bun", "buns", "tortilla", "wrap", "wraps"])) {
      return this.breadAlternatives();
    }
    if (this.containsAny(productContext, ["pasta", "spaghetti", "macaroni", "noodle", "noodles"])) {
      return this.pastaAlternatives();
    }
    if (this.containsAny(productContext, [
      "sauce",
      "sauces",
      "dressing",
      "dressings",
      "ketchup",
      "mayonnaise",
      "mayo",
      "marinara",
      "salsa",
    ])) {
      return this.sauceAlternatives();
    }
    if (this.containsAny(productContext, ["peanut butter", "almond butter", "cashew butter", "nut butter", "nut butters"])) {
      return this.nutButterAlternatives();
    }
    if (this.containsAny(productContext, ["milk", "milks", "almond milk", "oat milk", "soy milk", "plant milk", "plant milks"])) {
      return this.milkAlternatives();
    }
    if (this.containsAny(productContext, ["drink", "drinks", "beverage", "beverages", "water", "juice", "lemonade", "tea"])) {
      return this.beverageAlternatives();
    }
    return [];
  }

  sodaAlternatives() {
    return this.alternatives([
      {
        productName: "Vintage Cola",
        brand: "Olipop",
        reason: "Same cola-style shelf, with prebiotic fiber and much less sugar than regular soda.",
        healthSignal: "Related soda swap",
        retailerHint: "Available at Target, Walmart, Kroger, and Instacart",
      },
      {
        productName: "Classic Cola",
        brand: "Poppi",
        reason: "A real cola alternative positioned around lower sugar and prebiotics.",
        healthSignal: "Lower sugar cola",
        retailerHint: "Available at many grocery retailers",
      },
      {
        productName: "Zero Sugar Cola",
        brand: "Zevia",
        reason: "A zero-sugar cola option without synthetic colors.",
        healthSignal: "Zero sugar soda",
        retailerHint: "Available online and at major grocery stores",
      },
    ]);
  }

  beverageAlternatives() {
    return this.alternatives([
      {
        productName: "Sparkling Water",
        brand: "Spindrift",
        reason: "Same ready-to-drink category, made with sparkling water and real fruit juice.",
        healthSignal: "Cleaner beverage option",
        retailerHint: "Available at Target, Walmart, Kroger, and Instacart",
      },
      {
        productName: "Organic Juice Drink",
        brand: "Honest Kids",
        reason: "Related juice-box style option with lower sugar per pouch than many juice drinks.",
        healthSignal: "Lower sugar juice drink",
        retailerHint: "Available at Target, Walmart, Kroger, and Instacart",
      },
      {
        productName: "Coconut Water, No Sugar Added",
        brand: "Harmless Harvest",
        reason: "A simple beverage swap with no added sugar.",
        healthSignal: "No added sugar beverage",
        retailerHint: "Available online and in health grocery stores",
      },
    ]);
  }

  barAlternatives() {
    return this.alternatives([
      {
        productName: "Chocolate Sea Salt Protein Bar",
        brand: "RXBAR",
        reason: "Same snack-bar format with a short, recognizable ingredient list.",
        healthSignal: "Short ingredient bar",
        retailerHint: "Available online and near many grocery stores",
      },
      {
        productName: "Peanut Butter Chocolate Chip Bar",
        brand: "Larabar",
        reason: "A related bar made from dates, nuts, and chocolate chips.",
        healthSignal: "Simple snack bar",
        retailerHint: "Available at Target, Walmart, Kroger, and Instacart",
      },
      {
        productName: "Organic Plant Based Protein Bar",
        brand: "ALOHA",
        reason: "A bar-category alternative with plant protein and fiber.",
        healthSignal: "Plant protein bar",
        retailerHint: "Available online and at select grocery retailers",
      },
    ]);
  }

  cerealAlternatives() {
    return this.alternatives([
      {
        productName: "Unsweetened Cereal",
        brand: "Three Wishes",
        reason: "Same breakfast-cereal aisle, with no added sugar and more protein.",
        healthSignal: "Lower sugar cereal",
        retailerHint: "Available online and at select grocery retailers",
      },
      {
        productName: "Heritage Flakes",
        brand: "Nature's Path",
        reason: "A related flake cereal built around whole grains.",
        healthSignal: "Whole grain cereal",
        retailerHint: "Available at Target, Walmart, Kroger, and Instacart",
      },
      {
        productName: "Ezekiel 4:9 Sprouted Grain Cereal",
        brand: "Food For Life",
        reason: "A cereal alternative made with sprouted grains and legumes.",
        healthSignal: "Sprouted grain cereal",
        retailerHint: "Available online and in health grocery stores",
      },
    ]);
  }

  granolaAlternatives() {
    return this.alternatives([
      {
        productName: "Original Ancient Grain Granola",
        brand: "Purely Elizabeth",
        reason: "Same granola category, with recognizable grains, seeds, and nuts.",
        healthSignal: "Cleaner granola option",
        retailerHint: "Available at Target, Walmart, Kroger, and Instacart",
      },
      {
        productName: "Peanut Butter Granola",
        brand: "Michele's Granola",
        reason: "A related granola with a short ingredient list.",
        healthSignal: "Short ingredient granola",
        retailerHint: "Available online and at select grocery retailers",
      },
      {
        productName: "Organic Honey Almond Granola",
        brand: "Cascadian Farm",
        reason: "A grocery-accessible granola option with organic ingredients.",
        healthSignal: "Organic granola",
        retailerHint: "Available at major grocery stores",
      },
    ]);
  }

  yogurtAlternatives() {
    return this.alternatives([
      {
        productName: "Plain Skyr",
        brand: "Siggi's",
        reason: "Same yogurt case, with high protein and no added sugar in the plain version.",
        healthSignal: "Plain high protein yogurt",
        retailerHint: "Available at Target, Walmart, Kroger, and Instacart",
      },
      {
        productName: "Organic Plain Greek Yogurt",
        brand: "Stonyfield",
        reason: "A related plain yogurt option without sweet mix-ins.",
        healthSignal: "Plain organic yogurt",
        retailerHint: "Available at many grocery retailers",
      },
      {
        productName: "Less Sugar Greek Yogurt",
        brand: "Chobani",
        reason: "Same single-serve yogurt format with reduced sugar.",
        healthSignal: "Lower sugar yogurt",
        retailerHint: "Available at Target, Walmart, Kroger, and Instacart",
      },
    ]);
  }

  chocolateAlternatives() {
    return this.alternatives([
      {
        productName: "Simple Dark Chocolate",
        brand: "Hu",
        reason: "Same chocolate category, with a shorter ingredient list.",
        healthSignal: "Cleaner chocolate option",
        retailerHint: "Available online and at select grocery retailers",
      },
      {
        productName: "Dark Chocolate Gems",
        brand: "UNREAL",
        reason: "A related candy-coated chocolate with simpler colors and ingredients.",
        healthSignal: "Better candy swap",
        retailerHint: "Available at Target, Walmart, Kroger, and Instacart",
      },
      {
        productName: "Organic Dark Chocolate Bar",
        brand: "Alter Eco",
        reason: "A chocolate-bar alternative with organic ingredients.",
        healthSignal: "Organic chocolate",
        retailerHint: "Available online and in health grocery stores",
      },
    ]);
  }

  cookieAlternatives() {
    return this.alternatives([
      {
        productName: "Crunchy Chocolate Chip Cookies",
        brand: "Simple Mills",
        reason: "Same cookie shelf, made with a nut-and-seed flour blend.",
        healthSignal: "Cleaner cookie option",
        retailerHint: "Available at Target, Walmart, Kroger, and Instacart",
      },
      {
        productName: "Soft Baked Mini Cookies",
        brand: "MadeGood",
        reason: "A related cookie pack with organic ingredients.",
        healthSignal: "Organic cookie snack",
        retailerHint: "Available at many grocery retailers",
      },
      {
        productName: "Grain-Free Chocolate Chip Cookies",
        brand: "Hu",
        reason: "A cookie alternative with a shorter ingredient list.",
        healthSignal: "Short ingredient cookie",
        retailerHint: "Available online and at select grocery retailers",
      },
    ]);
  }

  snackAlternatives() {
    return this.alternatives([
      {
        productName: "Sea Salt Grain Free Tortilla Chips",
        brand: "Siete",
        reason: "Same chip/snack category, made with avocado oil and cassava flour.",
        healthSignal: "Better chip swap",
        retailerHint: "Available at Target, Walmart, Kroger, and Instacart",
      },
      {
        productName: "Himalayan Pink Salt Popcorn",
        brand: "LesserEvil",
        reason: "A related salty snack with a simple ingredient list.",
        healthSignal: "Simple salty snack",
        retailerHint: "Available at Target, Walmart, Kroger, and Instacart",
      },
      {
        productName: "Sea Salt Sweet Potato Chips",
        brand: "Jackson's",
        reason: "Same crunchy snack lane, made with sweet potatoes and avocado oil.",
        healthSignal: "Cleaner chip option",
        retailerHint: "Available online and at select grocery retailers",
      },
    ]);
  }

  breadAlternatives() {
    return this.alternatives([
      {
        productName: "Thin-Sliced 21 Whole Grains and Seeds",
        brand: "Dave's Killer Bread",
        reason: "Same bread aisle, with whole grains and seeds.",
        healthSignal: "Whole grain bread",
        retailerHint: "Available at Target, Walmart, Kroger, and Instacart",
      },
      {
        productName: "Ezekiel 4:9 Sprouted Grain Bread",
        brand: "Food For Life",
        reason: "A related bread made from sprouted grains and legumes.",
        healthSignal: "Sprouted grain bread",
        retailerHint: "Available online and in health grocery stores",
      },
      {
        productName: "Sprouted Power Bread",
        brand: "Silver Hills",
        reason: "Same loaf format with sprouted whole grains.",
        healthSignal: "Sprouted bread",
        retailerHint: "Available at select grocery retailers",
      },
    ]);
  }

  pastaAlternatives() {
    return this.alternatives([
      {
        productName: "Chickpea Pasta",
        brand: "Banza",
        reason: "Same pasta shape use case, with more protein and fiber from chickpeas.",
        healthSignal: "Higher protein pasta",
        retailerHint: "Available at Target, Walmart, Kroger, and Instacart",
      },
      {
        productName: "Whole Grain Spaghetti",
        brand: "Barilla",
        reason: "A related pasta option with whole grain wheat.",
        healthSignal: "Whole grain pasta",
        retailerHint: "Available at major grocery stores",
      },
      {
        productName: "Brown Rice Pasta",
        brand: "Jovial",
        reason: "Same pasta meal role, made with brown rice.",
        healthSignal: "Simple gluten-free pasta",
        retailerHint: "Available online and at select grocery retailers",
      },
    ]);
  }

  sauceAlternatives() {
    return this.alternatives([
      {
        productName: "Marinara Sauce",
        brand: "Rao's Homemade",
        reason: "Same sauce category, known for a short ingredient list and no added sugar.",
        healthSignal: "Cleaner pasta sauce",
        retailerHint: "Available at Target, Walmart, Kroger, and Instacart",
      },
      {
        productName: "Avocado Oil Ranch Dressing",
        brand: "Primal Kitchen",
        reason: "A related dressing made with avocado oil and no added sugar.",
        healthSignal: "Cleaner dressing",
        retailerHint: "Available online and at many grocery stores",
      },
      {
        productName: "Organic Apple Cider Vinaigrette",
        brand: "Bragg",
        reason: "Same dressing use case with a straightforward ingredient profile.",
        healthSignal: "Simple vinaigrette",
        retailerHint: "Available at major grocery stores",
      },
    ]);
  }

  nutButterAlternatives() {
    return this.alternatives([
      {
        productName: "Dark Roasted Creamy Peanut Butter",
        brand: "Santa Cruz Organic",
        reason: "Same nut-butter shelf, with organic peanuts and minimal ingredients.",
        healthSignal: "Simple peanut butter",
        retailerHint: "Available at Target, Walmart, Kroger, and Instacart",
      },
      {
        productName: "Organic Creamy Peanut Butter",
        brand: "Once Again",
        reason: "A related peanut butter with a short ingredient list.",
        healthSignal: "Organic nut butter",
        retailerHint: "Available online and in health grocery stores",
      },
      {
        productName: "Classic Almond Butter",
        brand: "Justin's",
        reason: "Same spread/snack role, made from dry roasted almonds.",
        healthSignal: "Almond butter swap",
        retailerHint: "Available at major grocery stores",
      },
    ]);
  }

  milkAlternatives() {
    return this.alternatives([
      {
        productName: "Unsweetened Almond Milk",
        brand: "MALK",
        reason: "Same milk alternative category, with a short ingredient list and no gums.",
        healthSignal: "Short ingredient milk",
        retailerHint: "Available at Whole Foods, Target, and select grocery retailers",
      },
      {
        productName: "Unsweetened Milked Oats",
        brand: "Elmhurst",
        reason: "A related oat milk with simple ingredients and no added gums.",
        healthSignal: "Simple oat milk",
        retailerHint: "Available online and at select grocery retailers",
      },
      {
        productName: "Unsweetened Almondmilk",
        brand: "Califia Farms",
        reason: "Same dairy-alternative use case with no added sugar.",
        healthSignal: "No added sugar milk",
        retailerHint: "Available at Target, Walmart, Kroger, and Instacart",
      },
    ]);
  }

  alternatives(items) {
    return items.map((item) => ({
      ...item,
      productUrl: `https://www.google.com/search?q=${encodeURIComponent(`${item.brand} ${item.productName}`)}`,
      imageUrl: "",
    }));
  }

  containsAny(context, terms) {
    const searchable = ` ${context} `;
    return terms.some((term) => searchable.includes(` ${this.normalize(term)} `));
  }

  normalize(value) {
    return String(value || "")
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, " ")
      .trim();
  }
}

module.exports = {
  MockRetailerProvider,
};
