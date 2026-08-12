package com.satya.calorietracker.data.seed

import com.satya.calorietracker.data.db.FoodEntity
import com.satya.calorietracker.domain.model.FoodSource
import com.satya.calorietracker.domain.model.MeasureUnit

/**
 * The offline food catalog: what you get before you've searched for anything and what
 * keeps working with the radio off.
 *
 * Weighted heavily towards Indian home cooking, because that is exactly what the online
 * databases are worst at — Open Food Facts is built around barcoded packaged goods, so
 * it can tell you about a protein bar but not about sambar.
 *
 * Values are per 100 g (or 100 ml for liquids) and come from standard composition tables
 * — IFCT 2017 for the Indian entries, USDA SR Legacy for the rest. Home cooking varies
 * enormously with how much oil goes in, so treat the cooked-dish numbers as sensible
 * averages rather than measurements. Every one of them can be edited, or replaced with
 * your own custom food.
 *
 * [VERSION] is bumped whenever entries are added, which is how [SeedSync] knows to
 * top up an existing install without touching anything you've logged.
 */
object SeedFoods {

    const val VERSION = 2

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

        // ==================================================== hot drinks
        Seed("Filter coffee with milk and sugar", 62.0, 1.9, 8.8, 2.1, 0.0, 8.2, 25.0, liquid = true, serving = 150.0, servingLabel = "1 tumbler (150 ml)"),
        Seed("Filter coffee, no sugar", 38.0, 1.9, 3.0, 1.9, 0.0, 2.8, 25.0, liquid = true, serving = 150.0, servingLabel = "1 tumbler (150 ml)"),
        Seed("Coffee with milk and sugar", 58.0, 1.8, 8.4, 1.9, 0.0, 7.9, 22.0, liquid = true, serving = 200.0, servingLabel = "1 cup (200 ml)"),
        Seed("Coffee with milk, no sugar", 35.0, 1.8, 2.6, 1.6, 0.0, 2.5, 22.0, liquid = true, serving = 200.0, servingLabel = "1 cup (200 ml)"),
        Seed("Black coffee", 2.0, 0.3, 0.0, 0.0, 0.0, 0.0, 5.0, liquid = true, serving = 240.0, servingLabel = "1 cup (240 ml)"),
        Seed("Masala chai with milk and sugar", 62.0, 1.9, 8.5, 2.2, 0.0, 8.0, 25.0, liquid = true, serving = 150.0, servingLabel = "1 cup (150 ml)"),
        Seed("Tea with milk, no sugar", 36.0, 1.8, 2.7, 1.7, 0.0, 2.6, 25.0, liquid = true, serving = 150.0, servingLabel = "1 cup (150 ml)"),
        Seed("Black tea, no sugar", 1.0, 0.0, 0.3, 0.0, 0.0, 0.0, 3.0, liquid = true, serving = 200.0, servingLabel = "1 cup (200 ml)"),
        Seed("Green tea", 1.0, 0.0, 0.2, 0.0, 0.0, 0.0, 1.0, liquid = true, serving = 240.0, servingLabel = "1 cup (240 ml)"),
        Seed("Badam milk", 95.0, 3.4, 12.0, 3.8, 0.3, 11.0, 45.0, liquid = true, serving = 200.0, servingLabel = "1 glass (200 ml)"),
        Seed("Horlicks / Boost with milk", 90.0, 3.6, 13.0, 2.4, 0.2, 11.5, 60.0, liquid = true, serving = 200.0, servingLabel = "1 glass (200 ml)"),

        // =================================================== cold drinks
        Seed("Buttermilk / chaas", 30.0, 1.6, 3.4, 1.0, 0.0, 3.2, 150.0, liquid = true, serving = 200.0, servingLabel = "1 glass (200 ml)"),
        Seed("Sweet lassi", 96.0, 3.0, 15.4, 2.6, 0.0, 14.5, 50.0, liquid = true, serving = 250.0, servingLabel = "1 glass (250 ml)"),
        Seed("Salted lassi", 56.0, 3.1, 5.0, 2.6, 0.0, 4.6, 220.0, liquid = true, serving = 250.0, servingLabel = "1 glass (250 ml)"),
        Seed("Mango lassi", 112.0, 2.8, 20.0, 2.4, 0.4, 18.5, 45.0, liquid = true, serving = 250.0, servingLabel = "1 glass (250 ml)"),
        Seed("Nimbu pani / lemonade", 40.0, 0.1, 10.2, 0.0, 0.1, 9.8, 25.0, liquid = true, serving = 250.0, servingLabel = "1 glass (250 ml)"),
        Seed("Sugarcane juice", 62.0, 0.2, 15.5, 0.1, 0.1, 14.8, 8.0, liquid = true, serving = 250.0, servingLabel = "1 glass (250 ml)"),
        Seed("Coconut water", 19.0, 0.7, 3.7, 0.2, 1.1, 2.6, 105.0, liquid = true, serving = 250.0, servingLabel = "1 glass (250 ml)"),
        Seed("Orange juice", 45.0, 0.7, 10.4, 0.2, 0.2, 8.4, 1.0, liquid = true, serving = 200.0, servingLabel = "1 glass (200 ml)"),
        Seed("Cola", 42.0, 0.0, 10.6, 0.0, 0.0, 10.6, 4.0, liquid = true, serving = 330.0, servingLabel = "1 can (330 ml)"),
        Seed("Water", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, liquid = true, serving = 250.0, servingLabel = "1 glass (250 ml)"),
        Seed("Beer", 43.0, 0.5, 3.6, 0.0, 0.0, 0.0, 4.0, liquid = true, serving = 330.0, servingLabel = "1 bottle (330 ml)"),
        Seed("Wine, red", 85.0, 0.1, 2.6, 0.0, 0.0, 0.6, 4.0, liquid = true, serving = 150.0, servingLabel = "1 glass (150 ml)"),
        Seed("Whisky / rum / vodka", 250.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, liquid = true, serving = 30.0, servingLabel = "1 peg (30 ml)"),

        // ============================================ South Indian tiffin
        Seed("Idli", 132.0, 3.8, 26.0, 0.9, 1.2, 0.4, 200.0, serving = 45.0, servingLabel = "1 idli (45 g)"),
        Seed("Plain dosa", 168.0, 3.9, 28.0, 4.5, 1.6, 0.6, 240.0, serving = 80.0, servingLabel = "1 dosa (80 g)"),
        Seed("Masala dosa", 200.0, 4.2, 30.0, 7.0, 2.2, 1.0, 300.0, serving = 150.0, servingLabel = "1 dosa (150 g)"),
        Seed("Rava dosa", 212.0, 4.0, 29.0, 9.0, 1.5, 0.8, 320.0, serving = 120.0, servingLabel = "1 dosa (120 g)"),
        Seed("Uttapam", 175.0, 4.5, 27.0, 5.5, 2.0, 1.6, 290.0, serving = 120.0, servingLabel = "1 uttapam (120 g)"),
        Seed("Medu vada", 300.0, 7.0, 32.0, 16.0, 3.5, 0.6, 350.0, serving = 45.0, servingLabel = "1 vada (45 g)"),
        Seed("Ven pongal", 180.0, 5.0, 26.0, 6.0, 2.0, 0.3, 320.0, serving = 200.0, servingLabel = "1 plate (200 g)"),
        Seed("Upma", 149.0, 3.5, 24.0, 4.4, 1.6, 1.0, 300.0, serving = 200.0, servingLabel = "1 bowl (200 g)"),
        Seed("Poha", 130.0, 2.6, 26.0, 2.2, 1.0, 0.9, 220.0, serving = 200.0, servingLabel = "1 plate (200 g)"),
        Seed("Idiyappam", 160.0, 3.0, 34.0, 1.0, 1.4, 0.2, 120.0),
        Seed("Appam", 150.0, 2.8, 28.0, 3.0, 1.0, 2.5, 150.0, serving = 90.0, servingLabel = "1 appam (90 g)"),
        Seed("Puttu", 170.0, 3.5, 34.0, 2.0, 2.5, 0.5, 90.0),
        Seed("Coconut chutney", 180.0, 3.0, 8.0, 15.0, 4.0, 2.0, 260.0, serving = 40.0, servingLabel = "2 tbsp (40 g)"),
        Seed("Tomato chutney", 90.0, 1.5, 8.0, 6.0, 1.8, 4.0, 320.0, serving = 40.0, servingLabel = "2 tbsp (40 g)"),
        Seed("Sambar", 85.0, 3.5, 12.0, 2.4, 3.0, 2.0, 380.0, serving = 150.0, servingLabel = "1 bowl (150 g)"),
        Seed("Rasam", 40.0, 1.5, 5.5, 1.4, 1.0, 1.2, 400.0, liquid = true, serving = 150.0, servingLabel = "1 bowl (150 ml)"),

        // ================================================ North breakfast
        Seed("Aloo paratha", 250.0, 5.5, 34.0, 10.0, 3.0, 1.2, 340.0, serving = 100.0, servingLabel = "1 paratha (100 g)"),
        Seed("Paratha, plain", 326.0, 7.0, 45.0, 13.0, 3.5, 1.2, 320.0, serving = 60.0, servingLabel = "1 paratha (60 g)"),
        Seed("Poori", 350.0, 6.5, 40.0, 18.0, 2.0, 0.8, 220.0, serving = 35.0, servingLabel = "1 poori (35 g)"),
        Seed("Chole bhature", 290.0, 8.0, 34.0, 13.0, 4.5, 2.0, 480.0, serving = 250.0, servingLabel = "1 plate (250 g)"),
        Seed("Egg bhurji", 180.0, 11.0, 4.0, 13.5, 0.6, 2.0, 380.0, serving = 150.0, servingLabel = "1 plate (150 g)"),
        Seed("Bread omelette", 220.0, 10.0, 22.0, 10.5, 1.5, 2.0, 420.0),
        Seed("Omelette, 2 eggs", 168.0, 11.0, 1.5, 13.0, 0.0, 0.8, 320.0, serving = 120.0, servingLabel = "2-egg omelette (120 g)"),
        Seed("Cornflakes with milk", 90.0, 3.0, 15.5, 1.6, 0.6, 8.0, 130.0, serving = 220.0, servingLabel = "1 bowl (220 g)"),
        Seed("Muesli, dry", 380.0, 10.0, 62.0, 9.0, 8.0, 18.0, 30.0, serving = 50.0, servingLabel = "50 g"),
        Seed("Oats porridge with milk", 90.0, 3.6, 13.0, 2.6, 1.6, 4.5, 40.0, serving = 250.0, servingLabel = "1 bowl (250 g)"),
        Seed("Rolled oats, dry", 379.0, 13.2, 67.7, 6.5, 10.1, 0.8, 6.0, serving = 40.0, servingLabel = "40 g dry"),

        // ========================================================= breads
        Seed("Chapati / roti", 297.0, 9.0, 51.0, 6.0, 4.9, 1.5, 190.0, serving = 40.0, servingLabel = "1 medium roti (40 g)"),
        Seed("Phulka, no oil", 285.0, 9.2, 53.0, 3.8, 5.0, 1.4, 150.0, serving = 35.0, servingLabel = "1 phulka (35 g)"),
        Seed("Naan", 310.0, 8.5, 50.0, 8.0, 2.2, 3.5, 420.0, serving = 90.0, servingLabel = "1 naan (90 g)"),
        Seed("Butter naan", 350.0, 8.2, 48.0, 13.5, 2.1, 3.4, 440.0, serving = 100.0, servingLabel = "1 naan (100 g)"),
        Seed("Tandoori roti", 280.0, 8.8, 52.0, 4.0, 4.5, 1.3, 300.0, serving = 60.0, servingLabel = "1 roti (60 g)"),
        Seed("Bhatura", 330.0, 7.5, 44.0, 14.0, 2.0, 2.0, 380.0, serving = 80.0, servingLabel = "1 bhatura (80 g)"),
        Seed("Jowar roti", 275.0, 8.0, 56.0, 2.5, 6.5, 1.0, 120.0, serving = 50.0, servingLabel = "1 roti (50 g)"),
        Seed("Bajra roti", 280.0, 9.0, 54.0, 4.0, 7.0, 1.0, 120.0, serving = 50.0, servingLabel = "1 roti (50 g)"),
        Seed("Ragi roti", 270.0, 7.0, 56.0, 2.0, 8.0, 1.0, 130.0, serving = 50.0, servingLabel = "1 roti (50 g)"),
        Seed("Missi roti", 300.0, 10.0, 48.0, 7.5, 6.0, 1.5, 320.0, serving = 55.0, servingLabel = "1 roti (55 g)"),
        Seed("Pav / bread roll", 280.0, 8.0, 52.0, 4.0, 2.2, 5.0, 480.0, serving = 45.0, servingLabel = "1 pav (45 g)"),
        Seed("Whole wheat bread", 247.0, 13.0, 41.0, 3.4, 7.0, 5.7, 400.0, serving = 30.0, servingLabel = "1 slice (30 g)"),
        Seed("White bread", 265.0, 9.0, 49.0, 3.2, 2.7, 5.0, 490.0, serving = 25.0, servingLabel = "1 slice (25 g)"),

        // ========================================================== rice
        Seed("White rice, cooked", 130.0, 2.7, 28.2, 0.3, 0.4, 0.1, 1.0, serving = 150.0, servingLabel = "1 cup cooked (150 g)"),
        Seed("Brown rice, cooked", 123.0, 2.7, 25.6, 1.0, 1.6, 0.2, 4.0, serving = 150.0, servingLabel = "1 cup cooked (150 g)"),
        Seed("Curd rice", 130.0, 3.6, 20.0, 3.6, 0.6, 2.0, 320.0, serving = 250.0, servingLabel = "1 bowl (250 g)"),
        Seed("Jeera rice", 160.0, 3.0, 28.0, 4.0, 0.8, 0.3, 300.0, serving = 200.0, servingLabel = "1 plate (200 g)"),
        Seed("Lemon rice", 170.0, 3.2, 28.0, 5.0, 1.2, 0.4, 380.0, serving = 200.0, servingLabel = "1 plate (200 g)"),
        Seed("Tamarind rice / puliyodarai", 180.0, 3.5, 29.0, 5.5, 1.5, 2.0, 420.0, serving = 200.0, servingLabel = "1 plate (200 g)"),
        Seed("Tomato rice", 165.0, 3.2, 28.0, 4.5, 1.3, 1.5, 360.0, serving = 200.0, servingLabel = "1 plate (200 g)"),
        Seed("Ghee rice", 200.0, 3.4, 30.0, 7.5, 0.8, 0.4, 320.0, serving = 200.0, servingLabel = "1 plate (200 g)"),
        Seed("Veg pulao", 155.0, 3.6, 25.0, 4.6, 1.8, 1.6, 330.0, serving = 200.0, servingLabel = "1 plate (200 g)"),
        Seed("Veg fried rice", 165.0, 3.5, 27.0, 4.8, 1.5, 1.2, 480.0, serving = 250.0, servingLabel = "1 plate (250 g)"),
        Seed("Chicken biryani", 190.0, 9.0, 24.0, 6.5, 1.4, 1.5, 450.0, serving = 250.0, servingLabel = "1 plate (250 g)"),
        Seed("Mutton biryani", 210.0, 10.0, 23.0, 9.0, 1.4, 1.5, 460.0, serving = 250.0, servingLabel = "1 plate (250 g)"),
        Seed("Veg biryani", 175.0, 4.0, 28.0, 5.5, 2.0, 1.8, 400.0, serving = 250.0, servingLabel = "1 plate (250 g)"),

        // ================================================= dals & legumes
        Seed("Toor dal, cooked", 121.0, 7.0, 20.0, 0.9, 5.0, 1.4, 240.0, serving = 150.0, servingLabel = "1 katori (150 g)"),
        Seed("Moong dal, cooked", 105.0, 7.0, 19.0, 0.4, 7.6, 2.0, 200.0, serving = 150.0, servingLabel = "1 katori (150 g)"),
        Seed("Chana dal, cooked", 120.0, 7.5, 19.0, 1.5, 6.0, 1.8, 230.0, serving = 150.0, servingLabel = "1 katori (150 g)"),
        Seed("Dal fry", 130.0, 6.5, 17.0, 4.0, 4.5, 1.5, 380.0, serving = 150.0, servingLabel = "1 katori (150 g)"),
        Seed("Dal tadka", 120.0, 6.0, 15.0, 4.0, 4.2, 1.5, 400.0, serving = 150.0, servingLabel = "1 katori (150 g)"),
        Seed("Dal makhani", 190.0, 7.0, 17.0, 10.5, 5.0, 2.0, 420.0, serving = 150.0, servingLabel = "1 katori (150 g)"),
        Seed("Rajma, cooked", 127.0, 8.7, 22.8, 0.5, 6.4, 0.3, 240.0, serving = 150.0, servingLabel = "1 katori (150 g)"),
        Seed("Chole / chana masala", 150.0, 7.5, 20.0, 4.5, 6.5, 3.0, 420.0, serving = 150.0, servingLabel = "1 katori (150 g)"),
        Seed("Chickpeas, boiled", 164.0, 8.9, 27.4, 2.6, 7.6, 4.8, 230.0),
        Seed("Kadhi", 90.0, 3.2, 8.0, 5.0, 1.0, 3.0, 380.0, serving = 150.0, servingLabel = "1 katori (150 g)"),
        Seed("Sprouts salad", 100.0, 7.0, 15.0, 0.8, 5.0, 2.0, 120.0, serving = 100.0, servingLabel = "1 katori (100 g)"),
        Seed("Lentils, cooked", 116.0, 9.0, 20.1, 0.4, 7.9, 1.8, 2.0),
        Seed("Soya chunks, dry", 345.0, 52.0, 33.0, 0.5, 13.0, 9.0, 3.0, serving = 30.0, servingLabel = "30 g dry"),

        // ================================================= paneer & sabzi
        Seed("Palak paneer", 180.0, 8.0, 7.0, 14.0, 2.5, 2.5, 420.0, serving = 180.0, servingLabel = "1 bowl (180 g)"),
        Seed("Paneer butter masala", 245.0, 9.0, 10.0, 19.0, 2.0, 5.0, 480.0, serving = 180.0, servingLabel = "1 bowl (180 g)"),
        Seed("Kadai paneer", 190.0, 9.5, 9.0, 13.5, 2.4, 4.0, 450.0, serving = 180.0, servingLabel = "1 bowl (180 g)"),
        Seed("Matar paneer", 170.0, 8.0, 11.0, 11.0, 3.0, 4.0, 430.0, serving = 180.0, servingLabel = "1 bowl (180 g)"),
        Seed("Malai kofta", 240.0, 6.5, 15.0, 17.5, 2.2, 5.0, 460.0, serving = 180.0, servingLabel = "1 bowl (180 g)"),
        Seed("Aloo gobi", 105.0, 2.5, 12.0, 5.5, 3.0, 3.0, 360.0, serving = 150.0, servingLabel = "1 katori (150 g)"),
        Seed("Bhindi masala", 120.0, 2.2, 9.0, 8.5, 3.4, 2.4, 340.0, serving = 150.0, servingLabel = "1 katori (150 g)"),
        Seed("Baingan bharta", 110.0, 2.0, 9.0, 7.5, 3.2, 4.0, 350.0, serving = 150.0, servingLabel = "1 katori (150 g)"),
        Seed("Mixed veg curry", 115.0, 3.0, 11.0, 6.5, 3.0, 3.5, 380.0, serving = 150.0, servingLabel = "1 katori (150 g)"),
        Seed("Aloo matar", 130.0, 3.5, 16.0, 6.0, 3.5, 2.5, 360.0, serving = 150.0, servingLabel = "1 katori (150 g)"),
        Seed("Jeera aloo", 140.0, 2.4, 19.0, 6.5, 2.4, 1.2, 330.0, serving = 150.0, servingLabel = "1 katori (150 g)"),
        Seed("Cabbage poriyal", 85.0, 2.0, 8.0, 5.0, 3.0, 3.0, 300.0, serving = 100.0, servingLabel = "1 katori (100 g)"),
        Seed("Beans poriyal", 90.0, 2.4, 8.5, 5.2, 3.5, 2.5, 300.0, serving = 100.0, servingLabel = "1 katori (100 g)"),
        Seed("Avial", 130.0, 2.6, 9.0, 9.5, 3.0, 3.0, 330.0, serving = 150.0, servingLabel = "1 katori (150 g)"),

        // ====================================================== non-veg
        Seed("Chicken curry", 180.0, 14.0, 6.0, 11.0, 1.5, 2.5, 420.0, serving = 200.0, servingLabel = "1 bowl (200 g)"),
        Seed("Butter chicken", 220.0, 14.5, 7.0, 15.0, 1.2, 4.0, 480.0, serving = 200.0, servingLabel = "1 bowl (200 g)"),
        Seed("Chicken tikka", 195.0, 24.0, 4.0, 9.0, 0.6, 2.0, 520.0, serving = 150.0, servingLabel = "1 plate (150 g)"),
        Seed("Tandoori chicken", 165.0, 25.0, 2.0, 6.5, 0.4, 1.0, 480.0, serving = 200.0, servingLabel = "1 quarter (200 g)"),
        Seed("Chicken 65", 250.0, 20.0, 12.0, 14.0, 0.8, 1.5, 620.0, serving = 150.0, servingLabel = "1 plate (150 g)"),
        Seed("Chicken kebab", 180.0, 22.0, 4.0, 8.5, 0.6, 1.2, 520.0),
        Seed("Chicken breast, cooked", 165.0, 31.0, 0.0, 3.6, 0.0, 0.0, 74.0, serving = 100.0, servingLabel = "100 g fillet"),
        Seed("Chicken thigh, cooked", 209.0, 26.0, 0.0, 10.9, 0.0, 0.0, 88.0),
        Seed("Egg curry", 150.0, 8.5, 6.0, 10.5, 1.2, 2.5, 400.0, serving = 200.0, servingLabel = "1 bowl (200 g)"),
        Seed("Mutton curry", 230.0, 16.0, 5.0, 16.5, 1.2, 2.0, 440.0, serving = 200.0, servingLabel = "1 bowl (200 g)"),
        Seed("Mutton keema", 210.0, 18.0, 4.0, 14.0, 1.0, 1.5, 430.0),
        Seed("Fish curry", 130.0, 13.0, 5.0, 6.5, 1.0, 2.0, 420.0, serving = 200.0, servingLabel = "1 bowl (200 g)"),
        Seed("Fish fry", 220.0, 20.0, 8.0, 12.0, 0.5, 0.5, 480.0, serving = 120.0, servingLabel = "1 piece (120 g)"),
        Seed("Prawn curry", 145.0, 14.0, 5.0, 8.0, 1.0, 2.0, 460.0, serving = 180.0, servingLabel = "1 bowl (180 g)"),
        Seed("Prawns, cooked", 99.0, 24.0, 0.2, 0.3, 0.0, 0.0, 111.0),
        Seed("Salmon, cooked", 208.0, 20.4, 0.0, 13.4, 0.0, 0.0, 59.0),
        Seed("Tuna, canned in water", 116.0, 25.5, 0.0, 0.8, 0.0, 0.0, 247.0),
        Seed("Egg, whole", 143.0, 12.6, 0.7, 9.5, 0.0, 0.4, 142.0, serving = 50.0, servingLabel = "1 large egg (50 g)"),
        Seed("Egg white", 52.0, 10.9, 0.7, 0.2, 0.0, 0.7, 166.0, serving = 33.0, servingLabel = "1 egg white (33 g)"),

        // ================================================ snacks & street
        Seed("Samosa", 308.0, 5.0, 32.0, 17.5, 3.0, 1.5, 420.0, serving = 60.0, servingLabel = "1 samosa (60 g)"),
        Seed("Pav bhaji", 180.0, 4.0, 22.0, 8.5, 3.5, 4.0, 520.0, serving = 300.0, servingLabel = "1 plate (300 g)"),
        Seed("Vada pav", 290.0, 6.5, 38.0, 12.5, 3.0, 3.0, 560.0, serving = 120.0, servingLabel = "1 vada pav (120 g)"),
        Seed("Pani puri", 130.0, 2.5, 20.0, 4.5, 1.8, 2.0, 380.0, serving = 120.0, servingLabel = "6 pieces (120 g)"),
        Seed("Bhel puri", 220.0, 5.0, 34.0, 7.5, 3.5, 5.0, 620.0, serving = 100.0, servingLabel = "1 plate (100 g)"),
        Seed("Sev puri", 250.0, 5.5, 33.0, 11.0, 3.2, 4.5, 640.0, serving = 100.0, servingLabel = "1 plate (100 g)"),
        Seed("Dhokla", 160.0, 6.0, 22.0, 5.0, 2.5, 4.0, 420.0, serving = 100.0, servingLabel = "3 pieces (100 g)"),
        Seed("Khandvi", 150.0, 5.5, 14.0, 8.0, 2.0, 2.5, 380.0),
        Seed("Pakora / bhaji", 310.0, 7.0, 26.0, 20.0, 3.5, 2.0, 440.0, serving = 60.0, servingLabel = "4 pieces (60 g)"),
        Seed("Aloo tikki", 200.0, 3.2, 26.0, 9.5, 2.6, 1.5, 400.0, serving = 80.0, servingLabel = "2 tikkis (80 g)"),
        Seed("Veg momos, steamed", 180.0, 5.0, 28.0, 5.0, 2.0, 2.0, 420.0, serving = 150.0, servingLabel = "6 pieces (150 g)"),
        Seed("Spring roll", 250.0, 5.0, 30.0, 12.0, 2.2, 3.0, 520.0),
        Seed("Murukku / chakli", 500.0, 8.0, 55.0, 27.0, 4.0, 1.0, 620.0, serving = 30.0, servingLabel = "30 g"),
        Seed("Namkeen mixture", 520.0, 12.0, 48.0, 30.0, 5.0, 3.0, 900.0, serving = 30.0, servingLabel = "30 g"),
        Seed("Aloo bhujia", 540.0, 11.0, 46.0, 34.0, 4.5, 2.0, 950.0, serving = 30.0, servingLabel = "30 g"),
        Seed("Papad, roasted", 350.0, 20.0, 48.0, 4.0, 8.0, 2.0, 1600.0, serving = 13.0, servingLabel = "1 papad (13 g)"),
        Seed("Masala peanuts", 550.0, 22.0, 24.0, 40.0, 7.0, 3.0, 600.0, serving = 30.0, servingLabel = "30 g"),
        Seed("Potato chips", 536.0, 7.0, 53.0, 34.6, 4.4, 0.3, 525.0, serving = 30.0, servingLabel = "small packet (30 g)"),
        Seed("French fries", 312.0, 3.4, 41.4, 14.7, 3.8, 0.3, 210.0, serving = 120.0, servingLabel = "1 portion (120 g)"),
        Seed("Pizza, cheese", 266.0, 11.0, 33.0, 10.0, 2.3, 3.6, 598.0, serving = 107.0, servingLabel = "1 slice (107 g)"),
        Seed("Burger, chicken", 250.0, 14.0, 25.0, 11.0, 1.5, 5.0, 500.0, serving = 170.0, servingLabel = "1 burger (170 g)"),
        Seed("Popcorn, air-popped", 387.0, 12.9, 77.9, 4.5, 14.5, 0.9, 8.0, serving = 25.0, servingLabel = "1 bowl (25 g)"),

        // ======================================================= sweets
        Seed("Gulab jamun", 300.0, 4.5, 42.0, 13.0, 0.4, 38.0, 90.0, serving = 40.0, servingLabel = "1 piece (40 g)"),
        Seed("Rasgulla", 190.0, 4.0, 38.0, 2.5, 0.0, 36.0, 60.0, serving = 50.0, servingLabel = "1 piece (50 g)"),
        Seed("Rasmalai", 250.0, 7.0, 32.0, 11.0, 0.2, 30.0, 90.0, serving = 60.0, servingLabel = "1 piece (60 g)"),
        Seed("Jalebi", 400.0, 3.0, 60.0, 17.0, 0.3, 50.0, 80.0, serving = 40.0, servingLabel = "1 piece (40 g)"),
        Seed("Besan ladoo", 420.0, 8.0, 52.0, 21.0, 3.0, 38.0, 60.0, serving = 40.0, servingLabel = "1 ladoo (40 g)"),
        Seed("Kaju katli", 480.0, 9.0, 55.0, 25.0, 1.5, 45.0, 40.0, serving = 20.0, servingLabel = "1 piece (20 g)"),
        Seed("Barfi", 400.0, 8.0, 50.0, 19.0, 0.5, 44.0, 80.0, serving = 30.0, servingLabel = "1 piece (30 g)"),
        Seed("Mysore pak", 500.0, 6.0, 52.0, 30.0, 1.5, 45.0, 50.0, serving = 30.0, servingLabel = "1 piece (30 g)"),
        Seed("Gajar halwa", 220.0, 3.5, 30.0, 10.0, 1.8, 26.0, 70.0, serving = 100.0, servingLabel = "1 katori (100 g)"),
        Seed("Sooji halwa", 350.0, 5.0, 48.0, 15.5, 1.2, 32.0, 60.0, serving = 100.0, servingLabel = "1 katori (100 g)"),
        Seed("Kheer / payasam", 130.0, 3.5, 20.0, 4.0, 0.4, 17.0, 60.0, serving = 150.0, servingLabel = "1 katori (150 g)"),
        Seed("Chikki", 450.0, 10.0, 55.0, 21.0, 4.0, 45.0, 30.0, serving = 25.0, servingLabel = "1 piece (25 g)"),
        Seed("Ice cream, vanilla", 207.0, 3.5, 23.6, 11.0, 0.7, 21.2, 80.0, serving = 66.0, servingLabel = "1 scoop (66 g)"),
        Seed("Dark chocolate 70%", 598.0, 7.8, 45.9, 42.6, 11.0, 24.0, 20.0, serving = 20.0, servingLabel = "2 squares (20 g)"),
        Seed("Milk chocolate", 535.0, 7.6, 59.4, 29.7, 3.4, 51.5, 79.0, serving = 25.0, servingLabel = "25 g"),
        Seed("Digestive biscuit", 480.0, 6.8, 62.0, 21.0, 3.3, 16.5, 600.0, serving = 15.0, servingLabel = "1 biscuit (15 g)"),
        Seed("Marie biscuit", 440.0, 7.0, 76.0, 12.0, 2.0, 22.0, 450.0, serving = 10.0, servingLabel = "2 biscuits (10 g)"),
        Seed("Rusk", 400.0, 9.0, 72.0, 9.0, 2.5, 18.0, 380.0, serving = 20.0, servingLabel = "2 rusks (20 g)"),

        // ======================================================== dairy
        Seed("Milk, full fat", 61.0, 3.2, 4.8, 3.3, 0.0, 5.1, 43.0, liquid = true, serving = 200.0, servingLabel = "1 glass (200 ml)"),
        Seed("Milk, toned", 47.0, 3.1, 4.7, 1.5, 0.0, 4.9, 44.0, liquid = true, serving = 200.0, servingLabel = "1 glass (200 ml)"),
        Seed("Curd / plain yoghurt", 61.0, 3.5, 4.7, 3.3, 0.0, 4.7, 46.0, serving = 150.0, servingLabel = "1 katori (150 g)"),
        Seed("Greek yoghurt, plain", 59.0, 10.2, 3.6, 0.4, 0.0, 3.2, 36.0, serving = 170.0, servingLabel = "1 pot (170 g)"),
        Seed("Paneer", 265.0, 18.3, 1.2, 20.8, 0.0, 1.2, 22.0, serving = 50.0, servingLabel = "50 g cube"),
        Seed("Tofu, firm", 144.0, 15.8, 4.3, 8.7, 2.3, 0.6, 14.0),
        Seed("Cheddar cheese", 403.0, 24.9, 1.3, 33.1, 0.0, 0.5, 621.0, serving = 20.0, servingLabel = "1 slice (20 g)"),
        Seed("Khoya / mawa", 420.0, 15.0, 25.0, 28.0, 0.0, 25.0, 90.0),
        Seed("Fresh cream", 290.0, 2.5, 3.0, 30.0, 0.0, 3.0, 35.0, serving = 15.0, servingLabel = "1 tbsp (15 g)"),
        Seed("Condensed milk", 320.0, 8.0, 55.0, 8.5, 0.0, 54.0, 130.0, serving = 20.0, servingLabel = "1 tbsp (20 g)"),
        Seed("Butter", 717.0, 0.9, 0.1, 81.1, 0.0, 0.1, 11.0, serving = 10.0, servingLabel = "1 tsp (10 g)"),
        Seed("Ghee", 900.0, 0.0, 0.0, 100.0, 0.0, 0.0, 0.0, serving = 10.0, servingLabel = "1 tsp (10 g)"),

        // ======================================================== fruits
        Seed("Banana", 89.0, 1.1, 22.8, 0.3, 2.6, 12.2, 1.0, serving = 118.0, servingLabel = "1 medium (118 g)"),
        Seed("Apple", 52.0, 0.3, 13.8, 0.2, 2.4, 10.4, 1.0, serving = 180.0, servingLabel = "1 medium (180 g)"),
        Seed("Orange", 47.0, 0.9, 11.8, 0.1, 2.4, 9.4, 0.0, serving = 130.0, servingLabel = "1 medium (130 g)"),
        Seed("Mango", 60.0, 0.8, 15.0, 0.4, 1.6, 13.7, 1.0, serving = 200.0, servingLabel = "1 medium (200 g)"),
        Seed("Papaya", 43.0, 0.5, 10.8, 0.3, 1.7, 7.8, 8.0, serving = 150.0, servingLabel = "1 bowl (150 g)"),
        Seed("Grapes", 69.0, 0.7, 18.1, 0.2, 0.9, 15.5, 2.0, serving = 100.0, servingLabel = "100 g"),
        Seed("Watermelon", 30.0, 0.6, 7.6, 0.2, 0.4, 6.2, 1.0, serving = 200.0, servingLabel = "1 bowl (200 g)"),
        Seed("Guava", 68.0, 2.6, 14.3, 1.0, 5.4, 8.9, 2.0, serving = 120.0, servingLabel = "1 medium (120 g)"),
        Seed("Pomegranate", 83.0, 1.7, 18.7, 1.2, 4.0, 13.7, 3.0, serving = 100.0, servingLabel = "1 bowl (100 g)"),
        Seed("Chikoo / sapota", 83.0, 0.4, 20.0, 1.1, 5.3, 15.0, 12.0),
        Seed("Custard apple / sitaphal", 94.0, 2.1, 24.0, 0.3, 2.4, 19.0, 4.0),
        Seed("Strawberries", 32.0, 0.7, 7.7, 0.3, 2.0, 4.9, 1.0),
        Seed("Pineapple", 50.0, 0.5, 13.1, 0.1, 1.4, 9.9, 1.0),
        Seed("Avocado", 160.0, 2.0, 8.5, 14.7, 6.7, 0.7, 7.0),
        Seed("Dates", 282.0, 2.5, 75.0, 0.4, 8.0, 63.0, 2.0, serving = 24.0, servingLabel = "3 dates (24 g)"),

        // ==================================================== vegetables
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
        Seed("Bottle gourd / lauki", 14.0, 0.6, 3.4, 0.0, 1.2, 1.6, 2.0),
        Seed("Green peas", 81.0, 5.4, 14.5, 0.4, 5.1, 5.7, 5.0),
        Seed("Beetroot", 43.0, 1.6, 9.6, 0.2, 2.8, 6.8, 78.0),
        Seed("Mixed salad leaves", 17.0, 1.4, 2.9, 0.2, 1.3, 0.8, 28.0),

        // ================================================== nuts & fats
        Seed("Almonds", 579.0, 21.2, 21.6, 49.9, 12.5, 4.4, 1.0, serving = 15.0, servingLabel = "10 almonds (15 g)"),
        Seed("Peanuts", 567.0, 25.8, 16.1, 49.2, 8.5, 4.7, 18.0, serving = 30.0, servingLabel = "30 g"),
        Seed("Cashews", 553.0, 18.2, 30.2, 43.9, 3.3, 5.9, 12.0, serving = 20.0, servingLabel = "10 cashews (20 g)"),
        Seed("Walnuts", 654.0, 15.2, 13.7, 65.2, 6.7, 2.6, 2.0, serving = 20.0, servingLabel = "20 g"),
        Seed("Pistachios", 560.0, 20.0, 28.0, 45.0, 10.0, 8.0, 1.0, serving = 25.0, servingLabel = "25 g"),
        Seed("Peanut butter", 588.0, 25.1, 20.0, 50.4, 6.0, 9.2, 429.0, serving = 16.0, servingLabel = "1 tbsp (16 g)"),
        Seed("Chia seeds", 486.0, 16.5, 42.1, 30.7, 34.4, 0.0, 16.0, serving = 15.0, servingLabel = "1 tbsp (15 g)"),
        Seed("Flax seeds", 534.0, 18.3, 28.9, 42.2, 27.3, 1.6, 30.0, serving = 10.0, servingLabel = "1 tbsp (10 g)"),
        Seed("Olive oil", 884.0, 0.0, 0.0, 100.0, 0.0, 0.0, 2.0, liquid = true, serving = 10.0, servingLabel = "1 tbsp (10 ml)"),
        Seed("Sunflower oil", 884.0, 0.0, 0.0, 100.0, 0.0, 0.0, 0.0, liquid = true, serving = 10.0, servingLabel = "1 tbsp (10 ml)"),
        Seed("Coconut oil", 892.0, 0.0, 0.0, 100.0, 0.0, 0.0, 0.0, liquid = true, serving = 10.0, servingLabel = "1 tbsp (10 ml)"),
        Seed("Mustard oil", 884.0, 0.0, 0.0, 100.0, 0.0, 0.0, 0.0, liquid = true, serving = 10.0, servingLabel = "1 tbsp (10 ml)"),

        // ============================================ grains & staples
        Seed("Pasta, cooked", 158.0, 5.8, 30.9, 0.9, 1.8, 0.6, 1.0),
        Seed("Quinoa, cooked", 120.0, 4.4, 21.3, 1.9, 2.8, 0.9, 7.0),
        Seed("Wheat flour / atta", 340.0, 12.0, 69.0, 1.7, 11.0, 0.4, 2.0),
        Seed("Besan / gram flour", 387.0, 22.0, 58.0, 6.7, 11.0, 11.0, 64.0),
        Seed("Rava / semolina", 360.0, 12.7, 73.0, 1.1, 3.9, 0.0, 1.0),
        Seed("Sugar, white", 387.0, 0.0, 100.0, 0.0, 0.0, 100.0, 1.0, serving = 4.0, servingLabel = "1 tsp (4 g)"),
        Seed("Jaggery / gur", 383.0, 0.4, 98.0, 0.1, 0.0, 85.0, 30.0, serving = 10.0, servingLabel = "10 g"),
        Seed("Honey", 304.0, 0.3, 82.4, 0.0, 0.2, 82.1, 4.0, serving = 21.0, servingLabel = "1 tbsp (21 g)"),

        // ================================================= supplements
        Seed("Whey protein powder", 380.0, 76.0, 8.0, 4.5, 0.5, 4.0, 300.0, serving = 30.0, servingLabel = "1 scoop (30 g)"),
        Seed("Casein protein powder", 360.0, 72.0, 9.0, 3.0, 1.0, 3.5, 400.0, serving = 30.0, servingLabel = "1 scoop (30 g)"),
        Seed("Mass gainer powder", 380.0, 20.0, 70.0, 3.0, 2.0, 25.0, 250.0, serving = 100.0, servingLabel = "1 scoop (100 g)"),
        Seed("Protein bar", 380.0, 30.0, 35.0, 11.0, 6.0, 4.0, 250.0, serving = 60.0, servingLabel = "1 bar (60 g)"),
        Seed("Grilled chicken salad", 120.0, 14.0, 5.0, 5.0, 1.8, 2.4, 260.0, serving = 250.0, servingLabel = "1 bowl (250 g)")
    )

    fun entities(now: Long = System.currentTimeMillis()): List<FoodEntity> =
        SEEDS.map { s ->
            FoodEntity(
                name = s.name,
                brand = null,
                barcode = null,
                sourceId = FoodSource.LOCAL.id,
                providerId = "local",
                // Stable key: lets SeedSync re-run without ever creating duplicates.
                providerRef = "local:" + s.name.lowercase()
                    .replace(Regex("[^a-z0-9]+"), "_")
                    .trim('_'),
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
