package com.satya.calorietracker.data.seed

import com.satya.calorietracker.data.db.FoodEntity
import com.satya.calorietracker.domain.model.FoodSource
import com.satya.calorietracker.domain.model.MeasureUnit

/**
 * A small, hand-checked offline catalog so the app is useful the second it is installed
 * and stays useful with the radio off. Values are per 100 g / 100 ml and come from
 * standard composition tables (USDA SR Legacy and IFCT for the Indian staples).
 *
 * These are rounded reference values, not lab measurements — fine for tracking trends,
 * and every one of them can be edited or replaced with your own custom food.
 */
object SeedFoods {

    private data class Seed(
        val name: String,
        val kcal: Double,
        val protein: Double,
        val carbs: Double,
        val fat: Double,
        val fiber: Double = 0.0,
        val sugar: Double = 0.0,
        val sodium: Double = 0.0,
        val liquid: Boolean = false,
        val serving: Double? = null,
        val servingLabel: String? = null
    )

    private val SEEDS: List<Seed> = listOf(
        // ---------------------------------------------------------- protein
        Seed("Chicken breast, cooked", 165.0, 31.0, 0.0, 3.6, 0.0, 0.0, 74.0, serving = 100.0, servingLabel = "100 g fillet"),
        Seed("Chicken thigh, cooked", 209.0, 26.0, 0.0, 10.9, 0.0, 0.0, 88.0),
        Seed("Egg, whole", 143.0, 12.6, 0.7, 9.5, 0.0, 0.4, 142.0, serving = 50.0, servingLabel = "1 large egg (50 g)"),
        Seed("Egg white", 52.0, 10.9, 0.7, 0.2, 0.0, 0.7, 166.0, serving = 33.0, servingLabel = "1 egg white (33 g)"),
        Seed("Paneer", 265.0, 18.3, 1.2, 20.8, 0.0, 1.2, 22.0, serving = 50.0, servingLabel = "50 g cube"),
        Seed("Tofu, firm", 144.0, 15.8, 4.3, 8.7, 2.3, 0.6, 14.0),
        Seed("Salmon, cooked", 208.0, 20.4, 0.0, 13.4, 0.0, 0.0, 59.0),
        Seed("Tuna, canned in water", 116.0, 25.5, 0.0, 0.8, 0.0, 0.0, 247.0),
        Seed("Prawns, cooked", 99.0, 24.0, 0.2, 0.3, 0.0, 0.0, 111.0),
        Seed("Mutton, cooked", 258.0, 25.6, 0.0, 16.5, 0.0, 0.0, 72.0),
        Seed("Whey protein powder", 380.0, 76.0, 8.0, 4.5, 0.5, 4.0, 300.0, serving = 30.0, servingLabel = "1 scoop (30 g)"),
        Seed("Soya chunks, dry", 345.0, 52.0, 33.0, 0.5, 13.0, 9.0, 3.0),

        // ------------------------------------------------------------ grains
        Seed("White rice, cooked", 130.0, 2.7, 28.2, 0.3, 0.4, 0.1, 1.0, serving = 150.0, servingLabel = "1 cup cooked (150 g)"),
        Seed("Brown rice, cooked", 123.0, 2.7, 25.6, 1.0, 1.6, 0.2, 4.0),
        Seed("Chapati / roti", 297.0, 9.0, 51.0, 6.0, 4.9, 1.5, 190.0, serving = 40.0, servingLabel = "1 medium roti (40 g)"),
        Seed("Whole wheat bread", 247.0, 13.0, 41.0, 3.4, 7.0, 5.7, 400.0, serving = 30.0, servingLabel = "1 slice (30 g)"),
        Seed("White bread", 265.0, 9.0, 49.0, 3.2, 2.7, 5.0, 490.0, serving = 25.0, servingLabel = "1 slice (25 g)"),
        Seed("Rolled oats, dry", 379.0, 13.2, 67.7, 6.5, 10.1, 0.8, 6.0, serving = 40.0, servingLabel = "40 g dry"),
        Seed("Idli", 132.0, 3.8, 26.0, 0.9, 1.2, 0.4, 200.0, serving = 45.0, servingLabel = "1 idli (45 g)"),
        Seed("Plain dosa", 168.0, 3.9, 28.0, 4.5, 1.6, 0.6, 240.0, serving = 80.0, servingLabel = "1 dosa (80 g)"),
        Seed("Poha, cooked", 130.0, 2.6, 26.0, 2.2, 1.0, 0.9, 220.0),
        Seed("Upma", 149.0, 3.5, 24.0, 4.4, 1.6, 1.0, 300.0),
        Seed("Pasta, cooked", 158.0, 5.8, 30.9, 0.9, 1.8, 0.6, 1.0),
        Seed("Quinoa, cooked", 120.0, 4.4, 21.3, 1.9, 2.8, 0.9, 7.0),
        Seed("Poori", 350.0, 6.5, 40.0, 18.0, 2.0, 0.8, 220.0, serving = 35.0, servingLabel = "1 poori (35 g)"),
        Seed("Paratha, plain", 326.0, 7.0, 45.0, 13.0, 3.5, 1.2, 320.0, serving = 60.0, servingLabel = "1 paratha (60 g)"),

        // ------------------------------------------------------------ pulses
        Seed("Toor dal, cooked", 121.0, 7.0, 20.0, 0.9, 5.0, 1.4, 240.0, serving = 150.0, servingLabel = "1 katori (150 g)"),
        Seed("Moong dal, cooked", 105.0, 7.0, 19.0, 0.4, 7.6, 2.0, 200.0),
        Seed("Rajma, cooked", 127.0, 8.7, 22.8, 0.5, 6.4, 0.3, 240.0),
        Seed("Chana / chickpeas, cooked", 164.0, 8.9, 27.4, 2.6, 7.6, 4.8, 230.0),
        Seed("Sambar", 85.0, 3.5, 12.0, 2.4, 3.0, 2.0, 380.0, serving = 150.0, servingLabel = "1 bowl (150 g)"),
        Seed("Lentils, cooked", 116.0, 9.0, 20.1, 0.4, 7.9, 1.8, 2.0),

        // ------------------------------------------------------------- dairy
        Seed("Milk, full fat", 61.0, 3.2, 4.8, 3.3, 0.0, 5.1, 43.0, liquid = true, serving = 200.0, servingLabel = "1 glass (200 ml)"),
        Seed("Milk, toned", 47.0, 3.1, 4.7, 1.5, 0.0, 4.9, 44.0, liquid = true, serving = 200.0, servingLabel = "1 glass (200 ml)"),
        Seed("Curd / plain yoghurt", 61.0, 3.5, 4.7, 3.3, 0.0, 4.7, 46.0, serving = 150.0, servingLabel = "1 katori (150 g)"),
        Seed("Greek yoghurt, plain", 59.0, 10.2, 3.6, 0.4, 0.0, 3.2, 36.0, serving = 170.0, servingLabel = "1 pot (170 g)"),
        Seed("Cheddar cheese", 403.0, 24.9, 1.3, 33.1, 0.0, 0.5, 621.0, serving = 20.0, servingLabel = "1 slice (20 g)"),
        Seed("Butter", 717.0, 0.9, 0.1, 81.1, 0.0, 0.1, 11.0, serving = 10.0, servingLabel = "1 tsp (10 g)"),
        Seed("Ghee", 900.0, 0.0, 0.0, 100.0, 0.0, 0.0, 0.0, serving = 10.0, servingLabel = "1 tsp (10 g)"),

        // ------------------------------------------------------------ fruits
        Seed("Banana", 89.0, 1.1, 22.8, 0.3, 2.6, 12.2, 1.0, serving = 118.0, servingLabel = "1 medium (118 g)"),
        Seed("Apple", 52.0, 0.3, 13.8, 0.2, 2.4, 10.4, 1.0, serving = 180.0, servingLabel = "1 medium (180 g)"),
        Seed("Orange", 47.0, 0.9, 11.8, 0.1, 2.4, 9.4, 0.0, serving = 130.0, servingLabel = "1 medium (130 g)"),
        Seed("Mango", 60.0, 0.8, 15.0, 0.4, 1.6, 13.7, 1.0),
        Seed("Papaya", 43.0, 0.5, 10.8, 0.3, 1.7, 7.8, 8.0),
        Seed("Grapes", 69.0, 0.7, 18.1, 0.2, 0.9, 15.5, 2.0),
        Seed("Watermelon", 30.0, 0.6, 7.6, 0.2, 0.4, 6.2, 1.0),
        Seed("Guava", 68.0, 2.6, 14.3, 1.0, 5.4, 8.9, 2.0),
        Seed("Pomegranate", 83.0, 1.7, 18.7, 1.2, 4.0, 13.7, 3.0),
        Seed("Strawberries", 32.0, 0.7, 7.7, 0.3, 2.0, 4.9, 1.0),
        Seed("Avocado", 160.0, 2.0, 8.5, 14.7, 6.7, 0.7, 7.0),
        Seed("Dates", 282.0, 2.5, 75.0, 0.4, 8.0, 63.0, 2.0, serving = 24.0, servingLabel = "3 dates (24 g)"),

        // --------------------------------------------------------- vegetables
        Seed("Potato, boiled", 87.0, 1.9, 20.1, 0.1, 1.8, 0.9, 4.0),
        Seed("Sweet potato, boiled", 76.0, 1.4, 17.7, 0.1, 2.5, 5.7, 27.0),
        Seed("Broccoli", 34.0, 2.8, 6.6, 0.4, 2.6, 1.7, 33.0),
        Seed("Spinach / palak", 23.0, 2.9, 3.6, 0.4, 2.2, 0.4, 79.0),
        Seed("Tomato", 18.0, 0.9, 3.9, 0.2, 1.2, 2.6, 5.0),
        Seed("Onion", 40.0, 1.1, 9.3, 0.1, 1.7, 4.2, 4.0),
        Seed("Carrot", 41.0, 0.9, 9.6, 0.2, 2.8, 4.7, 69.0),
        Seed("Cucumber", 15.0, 0.7, 3.6, 0.1, 0.5, 1.7, 2.0),
        Seed("Cauliflower / gobi", 25.0, 1.9, 5.0, 0.3, 2.0, 1.9, 30.0),
        Seed("Okra / bhindi", 33.0, 1.9, 7.5, 0.2, 3.2, 1.5, 7.0),
        Seed("Green peas", 81.0, 5.4, 14.5, 0.4, 5.1, 5.7, 5.0),
        Seed("Mixed salad leaves", 17.0, 1.4, 2.9, 0.2, 1.3, 0.8, 28.0),

        // ------------------------------------------------------- nuts & fats
        Seed("Almonds", 579.0, 21.2, 21.6, 49.9, 12.5, 4.4, 1.0, serving = 15.0, servingLabel = "10 almonds (15 g)"),
        Seed("Peanuts", 567.0, 25.8, 16.1, 49.2, 8.5, 4.7, 18.0),
        Seed("Cashews", 553.0, 18.2, 30.2, 43.9, 3.3, 5.9, 12.0),
        Seed("Walnuts", 654.0, 15.2, 13.7, 65.2, 6.7, 2.6, 2.0),
        Seed("Peanut butter", 588.0, 25.1, 20.0, 50.4, 6.0, 9.2, 429.0, serving = 16.0, servingLabel = "1 tbsp (16 g)"),
        Seed("Olive oil", 884.0, 0.0, 0.0, 100.0, 0.0, 0.0, 2.0, liquid = true, serving = 10.0, servingLabel = "1 tbsp (10 ml)"),
        Seed("Sunflower oil", 884.0, 0.0, 0.0, 100.0, 0.0, 0.0, 0.0, liquid = true, serving = 10.0, servingLabel = "1 tbsp (10 ml)"),
        Seed("Chia seeds", 486.0, 16.5, 42.1, 30.7, 34.4, 0.0, 16.0),

        // --------------------------------------------------------- beverages
        Seed("Water", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, liquid = true, serving = 250.0, servingLabel = "1 glass (250 ml)"),
        Seed("Black coffee", 2.0, 0.3, 0.0, 0.0, 0.0, 0.0, 5.0, liquid = true, serving = 240.0, servingLabel = "1 cup (240 ml)"),
        Seed("Masala chai with milk", 62.0, 1.9, 8.5, 2.2, 0.0, 8.0, 25.0, liquid = true, serving = 150.0, servingLabel = "1 cup (150 ml)"),
        Seed("Green tea", 1.0, 0.0, 0.2, 0.0, 0.0, 0.0, 1.0, liquid = true, serving = 240.0, servingLabel = "1 cup (240 ml)"),
        Seed("Orange juice", 45.0, 0.7, 10.4, 0.2, 0.2, 8.4, 1.0, liquid = true, serving = 200.0, servingLabel = "1 glass (200 ml)"),
        Seed("Cola", 42.0, 0.0, 10.6, 0.0, 0.0, 10.6, 4.0, liquid = true, serving = 330.0, servingLabel = "1 can (330 ml)"),
        Seed("Beer", 43.0, 0.5, 3.6, 0.0, 0.0, 0.0, 4.0, liquid = true, serving = 330.0, servingLabel = "1 bottle (330 ml)"),
        Seed("Coconut water", 19.0, 0.7, 3.7, 0.2, 1.1, 2.6, 105.0, liquid = true, serving = 250.0, servingLabel = "1 glass (250 ml)"),

        // ----------------------------------------------------- cooked dishes
        Seed("Chicken curry", 180.0, 14.0, 6.0, 11.0, 1.5, 2.5, 420.0, serving = 200.0, servingLabel = "1 bowl (200 g)"),
        Seed("Paneer butter masala", 245.0, 9.0, 10.0, 19.0, 2.0, 5.0, 480.0, serving = 180.0, servingLabel = "1 bowl (180 g)"),
        Seed("Chicken biryani", 190.0, 9.0, 24.0, 6.5, 1.4, 1.5, 450.0, serving = 250.0, servingLabel = "1 plate (250 g)"),
        Seed("Veg pulao", 155.0, 3.6, 25.0, 4.6, 1.8, 1.6, 330.0),
        Seed("Aloo gobi", 105.0, 2.5, 12.0, 5.5, 3.0, 3.0, 360.0),
        Seed("Dal tadka", 120.0, 6.0, 15.0, 4.0, 4.2, 1.5, 400.0),
        Seed("Omelette, 2 eggs", 168.0, 11.0, 1.5, 13.0, 0.0, 0.8, 320.0, serving = 120.0, servingLabel = "2-egg omelette (120 g)"),
        Seed("Grilled chicken salad", 120.0, 14.0, 5.0, 5.0, 1.8, 2.4, 260.0),
        Seed("Pizza, cheese", 266.0, 11.0, 33.0, 10.0, 2.3, 3.6, 598.0, serving = 107.0, servingLabel = "1 slice (107 g)"),
        Seed("Burger, chicken", 250.0, 14.0, 25.0, 11.0, 1.5, 5.0, 500.0, serving = 170.0, servingLabel = "1 burger (170 g)"),
        Seed("French fries", 312.0, 3.4, 41.4, 14.7, 3.8, 0.3, 210.0),
        Seed("Samosa", 308.0, 5.0, 32.0, 17.5, 3.0, 1.5, 420.0, serving = 60.0, servingLabel = "1 samosa (60 g)"),

        // ------------------------------------------------------------ snacks
        Seed("Dark chocolate 70%", 598.0, 7.8, 45.9, 42.6, 11.0, 24.0, 20.0, serving = 20.0, servingLabel = "2 squares (20 g)"),
        Seed("Milk chocolate", 535.0, 7.6, 59.4, 29.7, 3.4, 51.5, 79.0),
        Seed("Potato chips", 536.0, 7.0, 53.0, 34.6, 4.4, 0.3, 525.0, serving = 30.0, servingLabel = "small packet (30 g)"),
        Seed("Digestive biscuit", 480.0, 6.8, 62.0, 21.0, 3.3, 16.5, 600.0, serving = 15.0, servingLabel = "1 biscuit (15 g)"),
        Seed("Protein bar", 380.0, 30.0, 35.0, 11.0, 6.0, 4.0, 250.0, serving = 60.0, servingLabel = "1 bar (60 g)"),
        Seed("Popcorn, air-popped", 387.0, 12.9, 77.9, 4.5, 14.5, 0.9, 8.0),
        Seed("Ice cream, vanilla", 207.0, 3.5, 23.6, 11.0, 0.7, 21.2, 80.0, serving = 66.0, servingLabel = "1 scoop (66 g)"),
        Seed("Honey", 304.0, 0.3, 82.4, 0.0, 0.2, 82.1, 4.0, serving = 21.0, servingLabel = "1 tbsp (21 g)"),
        Seed("Sugar, white", 387.0, 0.0, 100.0, 0.0, 0.0, 100.0, 1.0, serving = 4.0, servingLabel = "1 tsp (4 g)")
    )

    fun entities(now: Long = System.currentTimeMillis()): List<FoodEntity> =
        SEEDS.map { s ->
            FoodEntity(
                name = s.name,
                brand = null,
                barcode = null,
                sourceId = FoodSource.LOCAL.id,
                providerId = "local",
                providerRef = "local:" + s.name.lowercase().replace(' ', '_'),
                per = 100.0,
                perUnitId = if (s.liquid) MeasureUnit.MILLILITRE.id else MeasureUnit.GRAM.id,
                calories = s.kcal,
                protein = s.protein,
                carbs = s.carbs,
                fat = s.fat,
                fiber = s.fiber,
                sugar = s.sugar,
                sodium = s.sodium,
                servingSize = s.serving,
                servingLabel = s.servingLabel,
                isCustom = false,
                createdAt = now,
                cachedAt = 0L
            )
        }

    val count: Int get() = SEEDS.size
}
