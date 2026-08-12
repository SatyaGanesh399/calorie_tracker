package com.satya.calorietracker.data.seed

import com.satya.calorietracker.data.db.ExerciseEntity
import com.satya.calorietracker.domain.model.Equipment
import com.satya.calorietracker.domain.model.ExerciseCategory

/**
 * The bundled exercise library — around 250 movements covering barbell, dumbbell,
 * machine, cable, bodyweight, kettlebell and cardio work.
 *
 * Big enough that typing three letters almost always finds what you want, and every
 * entry carries its primary muscle and equipment so the picker can filter and so that
 * search matches "cable", "hamstring" or "row" equally well.
 *
 * Anything missing you can add yourself; custom exercises sit alongside these and are
 * never overwritten by [SeedSync].
 */
object SeedExercises {

    const val VERSION = 1

    private data class Ex(
        val name: String,
        val category: ExerciseCategory,
        val equipment: Equipment,
        val muscle: String,
        val weight: Boolean = true,
        val reps: Boolean = true,
        val duration: Boolean = false,
        val distance: Boolean = false
    )

    private fun lift(name: String, cat: ExerciseCategory, eq: Equipment, muscle: String) =
        Ex(name, cat, eq, muscle)

    /** Bodyweight movement: load is optional (belt, vest), reps are the point. */
    private fun body(name: String, cat: ExerciseCategory, muscle: String) =
        Ex(name, cat, Equipment.BODYWEIGHT, muscle, weight = true, reps = true)

    /** A hold — seconds, optionally weighted. */
    private fun hold(name: String, cat: ExerciseCategory, eq: Equipment, muscle: String) =
        Ex(name, cat, eq, muscle, weight = true, reps = false, duration = true)

    /** Time and distance, no load. */
    private fun cardio(name: String, muscle: String = "Full body", distance: Boolean = true) =
        Ex(
            name, ExerciseCategory.CARDIO, Equipment.CARDIO_MACHINE, muscle,
            weight = false, reps = false, duration = true, distance = distance
        )

    private val EXERCISES: List<Ex> = listOf(

        // ========================================================== chest
        lift("Bench press", ExerciseCategory.CHEST, Equipment.BARBELL, "Chest"),
        lift("Incline bench press", ExerciseCategory.CHEST, Equipment.BARBELL, "Upper chest"),
        lift("Decline bench press", ExerciseCategory.CHEST, Equipment.BARBELL, "Lower chest"),
        lift("Close-grip bench press", ExerciseCategory.CHEST, Equipment.BARBELL, "Triceps"),
        lift("Paused bench press", ExerciseCategory.CHEST, Equipment.BARBELL, "Chest"),
        lift("Floor press", ExerciseCategory.CHEST, Equipment.BARBELL, "Chest"),
        lift("Dumbbell bench press", ExerciseCategory.CHEST, Equipment.DUMBBELL, "Chest"),
        lift("Incline dumbbell press", ExerciseCategory.CHEST, Equipment.DUMBBELL, "Upper chest"),
        lift("Decline dumbbell press", ExerciseCategory.CHEST, Equipment.DUMBBELL, "Lower chest"),
        lift("Dumbbell flye", ExerciseCategory.CHEST, Equipment.DUMBBELL, "Chest"),
        lift("Incline dumbbell flye", ExerciseCategory.CHEST, Equipment.DUMBBELL, "Upper chest"),
        lift("Cable crossover", ExerciseCategory.CHEST, Equipment.CABLE, "Chest"),
        lift("Low cable crossover", ExerciseCategory.CHEST, Equipment.CABLE, "Upper chest"),
        lift("High cable crossover", ExerciseCategory.CHEST, Equipment.CABLE, "Lower chest"),
        lift("Cable flye", ExerciseCategory.CHEST, Equipment.CABLE, "Chest"),
        lift("Pec deck", ExerciseCategory.CHEST, Equipment.MACHINE, "Chest"),
        lift("Chest press machine", ExerciseCategory.CHEST, Equipment.MACHINE, "Chest"),
        lift("Incline chest press machine", ExerciseCategory.CHEST, Equipment.MACHINE, "Upper chest"),
        lift("Smith machine bench press", ExerciseCategory.CHEST, Equipment.MACHINE, "Chest"),
        lift("Incline Smith machine press", ExerciseCategory.CHEST, Equipment.MACHINE, "Upper chest"),
        lift("Landmine press", ExerciseCategory.CHEST, Equipment.BARBELL, "Chest"),
        lift("Svend press", ExerciseCategory.CHEST, Equipment.OTHER, "Chest"),
        body("Push-up", ExerciseCategory.CHEST, "Chest"),
        body("Wide push-up", ExerciseCategory.CHEST, "Chest"),
        body("Diamond push-up", ExerciseCategory.CHEST, "Triceps"),
        body("Decline push-up", ExerciseCategory.CHEST, "Upper chest"),
        body("Incline push-up", ExerciseCategory.CHEST, "Lower chest"),
        body("Chest dip", ExerciseCategory.CHEST, "Lower chest"),
        body("Weighted chest dip", ExerciseCategory.CHEST, "Lower chest"),

        // =========================================================== back
        lift("Deadlift", ExerciseCategory.BACK, Equipment.BARBELL, "Posterior chain"),
        lift("Sumo deadlift", ExerciseCategory.BACK, Equipment.BARBELL, "Posterior chain"),
        lift("Trap bar deadlift", ExerciseCategory.BACK, Equipment.BARBELL, "Posterior chain"),
        lift("Deficit deadlift", ExerciseCategory.BACK, Equipment.BARBELL, "Posterior chain"),
        lift("Snatch-grip deadlift", ExerciseCategory.BACK, Equipment.BARBELL, "Upper back"),
        lift("Rack pull", ExerciseCategory.BACK, Equipment.BARBELL, "Upper back"),
        lift("Barbell row", ExerciseCategory.BACK, Equipment.BARBELL, "Lats"),
        lift("Pendlay row", ExerciseCategory.BACK, Equipment.BARBELL, "Lats"),
        lift("Landmine row", ExerciseCategory.BACK, Equipment.BARBELL, "Lats"),
        lift("T-bar row", ExerciseCategory.BACK, Equipment.MACHINE, "Lats"),
        lift("Chest-supported T-bar row", ExerciseCategory.BACK, Equipment.MACHINE, "Lats"),
        lift("Dumbbell row", ExerciseCategory.BACK, Equipment.DUMBBELL, "Lats"),
        lift("Chest-supported dumbbell row", ExerciseCategory.BACK, Equipment.DUMBBELL, "Lats"),
        lift("Seal row", ExerciseCategory.BACK, Equipment.DUMBBELL, "Lats"),
        lift("Meadows row", ExerciseCategory.BACK, Equipment.BARBELL, "Lats"),
        lift("Kroc row", ExerciseCategory.BACK, Equipment.DUMBBELL, "Lats"),
        lift("Lat pulldown", ExerciseCategory.BACK, Equipment.CABLE, "Lats"),
        lift("Close-grip lat pulldown", ExerciseCategory.BACK, Equipment.CABLE, "Lats"),
        lift("Reverse-grip lat pulldown", ExerciseCategory.BACK, Equipment.CABLE, "Lats"),
        lift("Straight-arm pulldown", ExerciseCategory.BACK, Equipment.CABLE, "Lats"),
        lift("Seated cable row", ExerciseCategory.BACK, Equipment.CABLE, "Mid back"),
        lift("Single-arm cable row", ExerciseCategory.BACK, Equipment.CABLE, "Lats"),
        lift("Cable rear delt row", ExerciseCategory.BACK, Equipment.CABLE, "Rear delts"),
        lift("Machine row", ExerciseCategory.BACK, Equipment.MACHINE, "Mid back"),
        lift("Smith machine row", ExerciseCategory.BACK, Equipment.MACHINE, "Lats"),
        lift("Face pull", ExerciseCategory.BACK, Equipment.CABLE, "Rear delts"),
        lift("Barbell shrug", ExerciseCategory.BACK, Equipment.BARBELL, "Traps"),
        lift("Dumbbell shrug", ExerciseCategory.BACK, Equipment.DUMBBELL, "Traps"),
        lift("Cable pull-through", ExerciseCategory.BACK, Equipment.CABLE, "Glutes"),
        lift("Good morning", ExerciseCategory.BACK, Equipment.BARBELL, "Hamstrings"),
        lift("Jefferson curl", ExerciseCategory.BACK, Equipment.BARBELL, "Spinal erectors"),
        lift("Reverse hyperextension", ExerciseCategory.BACK, Equipment.MACHINE, "Glutes"),
        lift("Band pull-apart", ExerciseCategory.BACK, Equipment.BAND, "Rear delts"),
        body("Pull-up", ExerciseCategory.BACK, "Lats"),
        body("Chin-up", ExerciseCategory.BACK, "Lats"),
        body("Wide-grip pull-up", ExerciseCategory.BACK, "Lats"),
        body("Neutral-grip pull-up", ExerciseCategory.BACK, "Lats"),
        body("Weighted pull-up", ExerciseCategory.BACK, "Lats"),
        body("Weighted chin-up", ExerciseCategory.BACK, "Lats"),
        body("Assisted pull-up", ExerciseCategory.BACK, "Lats"),
        body("Scapular pull-up", ExerciseCategory.BACK, "Lats"),
        body("Inverted row", ExerciseCategory.BACK, "Mid back"),
        body("Back extension", ExerciseCategory.BACK, "Spinal erectors"),
        body("Hyperextension", ExerciseCategory.BACK, "Spinal erectors"),
        hold("Dead hang", ExerciseCategory.BACK, Equipment.BODYWEIGHT, "Grip"),

        // =========================================================== legs
        lift("Back squat", ExerciseCategory.LEGS, Equipment.BARBELL, "Quads"),
        lift("Front squat", ExerciseCategory.LEGS, Equipment.BARBELL, "Quads"),
        lift("High-bar squat", ExerciseCategory.LEGS, Equipment.BARBELL, "Quads"),
        lift("Low-bar squat", ExerciseCategory.LEGS, Equipment.BARBELL, "Quads"),
        lift("Box squat", ExerciseCategory.LEGS, Equipment.BARBELL, "Quads"),
        lift("Pause squat", ExerciseCategory.LEGS, Equipment.BARBELL, "Quads"),
        lift("Zercher squat", ExerciseCategory.LEGS, Equipment.BARBELL, "Quads"),
        lift("Overhead squat", ExerciseCategory.LEGS, Equipment.BARBELL, "Quads"),
        lift("Sumo squat", ExerciseCategory.LEGS, Equipment.BARBELL, "Adductors"),
        lift("Goblet squat", ExerciseCategory.LEGS, Equipment.DUMBBELL, "Quads"),
        lift("Hack squat", ExerciseCategory.LEGS, Equipment.MACHINE, "Quads"),
        lift("Smith machine squat", ExerciseCategory.LEGS, Equipment.MACHINE, "Quads"),
        lift("Belt squat", ExerciseCategory.LEGS, Equipment.MACHINE, "Quads"),
        lift("Leg press", ExerciseCategory.LEGS, Equipment.MACHINE, "Quads"),
        lift("Single-leg press", ExerciseCategory.LEGS, Equipment.MACHINE, "Quads"),
        lift("Bulgarian split squat", ExerciseCategory.LEGS, Equipment.DUMBBELL, "Quads"),
        lift("Split squat", ExerciseCategory.LEGS, Equipment.DUMBBELL, "Quads"),
        lift("Walking lunge", ExerciseCategory.LEGS, Equipment.DUMBBELL, "Quads"),
        lift("Reverse lunge", ExerciseCategory.LEGS, Equipment.DUMBBELL, "Glutes"),
        lift("Forward lunge", ExerciseCategory.LEGS, Equipment.DUMBBELL, "Quads"),
        lift("Step-up", ExerciseCategory.LEGS, Equipment.DUMBBELL, "Quads"),
        lift("Leg extension", ExerciseCategory.LEGS, Equipment.MACHINE, "Quads"),
        lift("Lying leg curl", ExerciseCategory.LEGS, Equipment.MACHINE, "Hamstrings"),
        lift("Seated leg curl", ExerciseCategory.LEGS, Equipment.MACHINE, "Hamstrings"),
        lift("Romanian deadlift", ExerciseCategory.LEGS, Equipment.BARBELL, "Hamstrings"),
        lift("Dumbbell Romanian deadlift", ExerciseCategory.LEGS, Equipment.DUMBBELL, "Hamstrings"),
        lift("Single-leg Romanian deadlift", ExerciseCategory.LEGS, Equipment.DUMBBELL, "Hamstrings"),
        lift("Stiff-leg deadlift", ExerciseCategory.LEGS, Equipment.BARBELL, "Hamstrings"),
        lift("Hip thrust", ExerciseCategory.LEGS, Equipment.BARBELL, "Glutes"),
        lift("Glute bridge", ExerciseCategory.LEGS, Equipment.BARBELL, "Glutes"),
        lift("Cable glute kickback", ExerciseCategory.LEGS, Equipment.CABLE, "Glutes"),
        lift("Hip abduction machine", ExerciseCategory.LEGS, Equipment.MACHINE, "Glutes"),
        lift("Hip adduction machine", ExerciseCategory.LEGS, Equipment.MACHINE, "Adductors"),
        lift("Standing calf raise", ExerciseCategory.LEGS, Equipment.MACHINE, "Calves"),
        lift("Seated calf raise", ExerciseCategory.LEGS, Equipment.MACHINE, "Calves"),
        lift("Calf press", ExerciseCategory.LEGS, Equipment.MACHINE, "Calves"),
        lift("Donkey calf raise", ExerciseCategory.LEGS, Equipment.MACHINE, "Calves"),
        body("Bodyweight squat", ExerciseCategory.LEGS, "Quads"),
        body("Pistol squat", ExerciseCategory.LEGS, "Quads"),
        body("Sissy squat", ExerciseCategory.LEGS, "Quads"),
        body("Nordic hamstring curl", ExerciseCategory.LEGS, "Hamstrings"),
        hold("Wall sit", ExerciseCategory.LEGS, Equipment.BODYWEIGHT, "Quads"),

        // ====================================================== shoulders
        lift("Overhead press", ExerciseCategory.SHOULDERS, Equipment.BARBELL, "Front delts"),
        lift("Seated overhead press", ExerciseCategory.SHOULDERS, Equipment.BARBELL, "Front delts"),
        lift("Military press", ExerciseCategory.SHOULDERS, Equipment.BARBELL, "Front delts"),
        lift("Push press", ExerciseCategory.SHOULDERS, Equipment.BARBELL, "Front delts"),
        lift("Behind-the-neck press", ExerciseCategory.SHOULDERS, Equipment.BARBELL, "Side delts"),
        lift("Bradford press", ExerciseCategory.SHOULDERS, Equipment.BARBELL, "Side delts"),
        lift("Dumbbell shoulder press", ExerciseCategory.SHOULDERS, Equipment.DUMBBELL, "Front delts"),
        lift("Arnold press", ExerciseCategory.SHOULDERS, Equipment.DUMBBELL, "Front delts"),
        lift("Lateral raise", ExerciseCategory.SHOULDERS, Equipment.DUMBBELL, "Side delts"),
        lift("Cable lateral raise", ExerciseCategory.SHOULDERS, Equipment.CABLE, "Side delts"),
        lift("Machine lateral raise", ExerciseCategory.SHOULDERS, Equipment.MACHINE, "Side delts"),
        lift("Landmine lateral raise", ExerciseCategory.SHOULDERS, Equipment.BARBELL, "Side delts"),
        lift("Front raise", ExerciseCategory.SHOULDERS, Equipment.DUMBBELL, "Front delts"),
        lift("Rear delt flye", ExerciseCategory.SHOULDERS, Equipment.DUMBBELL, "Rear delts"),
        lift("Reverse pec deck", ExerciseCategory.SHOULDERS, Equipment.MACHINE, "Rear delts"),
        lift("Upright row", ExerciseCategory.SHOULDERS, Equipment.BARBELL, "Side delts"),
        lift("Cuban press", ExerciseCategory.SHOULDERS, Equipment.DUMBBELL, "Rotator cuff"),
        lift("Y-raise", ExerciseCategory.SHOULDERS, Equipment.DUMBBELL, "Rear delts"),
        lift("Shoulder press machine", ExerciseCategory.SHOULDERS, Equipment.MACHINE, "Front delts"),
        body("Pike push-up", ExerciseCategory.SHOULDERS, "Front delts"),
        body("Handstand push-up", ExerciseCategory.SHOULDERS, "Front delts"),

        // =========================================================== arms
        lift("Barbell curl", ExerciseCategory.ARMS, Equipment.BARBELL, "Biceps"),
        lift("EZ-bar curl", ExerciseCategory.ARMS, Equipment.BARBELL, "Biceps"),
        lift("Dumbbell curl", ExerciseCategory.ARMS, Equipment.DUMBBELL, "Biceps"),
        lift("Alternating dumbbell curl", ExerciseCategory.ARMS, Equipment.DUMBBELL, "Biceps"),
        lift("Hammer curl", ExerciseCategory.ARMS, Equipment.DUMBBELL, "Brachialis"),
        lift("Incline dumbbell curl", ExerciseCategory.ARMS, Equipment.DUMBBELL, "Biceps"),
        lift("Preacher curl", ExerciseCategory.ARMS, Equipment.BARBELL, "Biceps"),
        lift("Concentration curl", ExerciseCategory.ARMS, Equipment.DUMBBELL, "Biceps"),
        lift("Spider curl", ExerciseCategory.ARMS, Equipment.DUMBBELL, "Biceps"),
        lift("Cable curl", ExerciseCategory.ARMS, Equipment.CABLE, "Biceps"),
        lift("Rope hammer curl", ExerciseCategory.ARMS, Equipment.CABLE, "Brachialis"),
        lift("Reverse curl", ExerciseCategory.ARMS, Equipment.BARBELL, "Forearms"),
        lift("Zottman curl", ExerciseCategory.ARMS, Equipment.DUMBBELL, "Forearms"),
        lift("Drag curl", ExerciseCategory.ARMS, Equipment.BARBELL, "Biceps"),
        lift("Machine curl", ExerciseCategory.ARMS, Equipment.MACHINE, "Biceps"),
        lift("Triceps pushdown", ExerciseCategory.ARMS, Equipment.CABLE, "Triceps"),
        lift("Rope pushdown", ExerciseCategory.ARMS, Equipment.CABLE, "Triceps"),
        lift("Overhead triceps extension", ExerciseCategory.ARMS, Equipment.CABLE, "Triceps"),
        lift("Dumbbell overhead extension", ExerciseCategory.ARMS, Equipment.DUMBBELL, "Triceps"),
        lift("Skull crusher", ExerciseCategory.ARMS, Equipment.BARBELL, "Triceps"),
        lift("JM press", ExerciseCategory.ARMS, Equipment.BARBELL, "Triceps"),
        lift("Triceps kickback", ExerciseCategory.ARMS, Equipment.DUMBBELL, "Triceps"),
        lift("Triceps machine", ExerciseCategory.ARMS, Equipment.MACHINE, "Triceps"),
        lift("Wrist curl", ExerciseCategory.ARMS, Equipment.BARBELL, "Forearms"),
        lift("Reverse wrist curl", ExerciseCategory.ARMS, Equipment.BARBELL, "Forearms"),
        body("Triceps dip", ExerciseCategory.ARMS, "Triceps"),
        body("Bench dip", ExerciseCategory.ARMS, "Triceps"),
        hold("Plate pinch", ExerciseCategory.ARMS, Equipment.OTHER, "Grip"),

        // =========================================================== core
        hold("Plank", ExerciseCategory.CORE, Equipment.BODYWEIGHT, "Abs"),
        hold("Side plank", ExerciseCategory.CORE, Equipment.BODYWEIGHT, "Obliques"),
        hold("Hollow hold", ExerciseCategory.CORE, Equipment.BODYWEIGHT, "Abs"),
        hold("Copenhagen plank", ExerciseCategory.CORE, Equipment.BODYWEIGHT, "Adductors"),
        body("Crunch", ExerciseCategory.CORE, "Abs"),
        body("Sit-up", ExerciseCategory.CORE, "Abs"),
        body("Bicycle crunch", ExerciseCategory.CORE, "Obliques"),
        body("Leg raise", ExerciseCategory.CORE, "Lower abs"),
        body("Hanging leg raise", ExerciseCategory.CORE, "Lower abs"),
        body("Hanging knee raise", ExerciseCategory.CORE, "Lower abs"),
        body("Toes to bar", ExerciseCategory.CORE, "Abs"),
        body("V-up", ExerciseCategory.CORE, "Abs"),
        body("Flutter kick", ExerciseCategory.CORE, "Lower abs"),
        body("Mountain climber", ExerciseCategory.CORE, "Abs"),
        body("Dead bug", ExerciseCategory.CORE, "Abs"),
        body("Bird dog", ExerciseCategory.CORE, "Abs"),
        body("Dragon flag", ExerciseCategory.CORE, "Abs"),
        body("Ab wheel rollout", ExerciseCategory.CORE, "Abs"),
        lift("Cable crunch", ExerciseCategory.CORE, Equipment.CABLE, "Abs"),
        lift("Standing cable crunch", ExerciseCategory.CORE, Equipment.CABLE, "Abs"),
        lift("Russian twist", ExerciseCategory.CORE, Equipment.OTHER, "Obliques"),
        lift("Woodchopper", ExerciseCategory.CORE, Equipment.CABLE, "Obliques"),
        lift("Pallof press", ExerciseCategory.CORE, Equipment.CABLE, "Obliques"),
        lift("Machine crunch", ExerciseCategory.CORE, Equipment.MACHINE, "Abs"),

        // ====================================================== full body
        lift("Clean and jerk", ExerciseCategory.FULL_BODY, Equipment.BARBELL, "Full body"),
        lift("Power clean", ExerciseCategory.FULL_BODY, Equipment.BARBELL, "Full body"),
        lift("Hang clean", ExerciseCategory.FULL_BODY, Equipment.BARBELL, "Full body"),
        lift("Clean and press", ExerciseCategory.FULL_BODY, Equipment.BARBELL, "Full body"),
        lift("Snatch", ExerciseCategory.FULL_BODY, Equipment.BARBELL, "Full body"),
        lift("Power snatch", ExerciseCategory.FULL_BODY, Equipment.BARBELL, "Full body"),
        lift("Clean pull", ExerciseCategory.FULL_BODY, Equipment.BARBELL, "Full body"),
        lift("Snatch pull", ExerciseCategory.FULL_BODY, Equipment.BARBELL, "Full body"),
        lift("Thruster", ExerciseCategory.FULL_BODY, Equipment.BARBELL, "Full body"),
        lift("Kettlebell swing", ExerciseCategory.FULL_BODY, Equipment.KETTLEBELL, "Posterior chain"),
        lift("Kettlebell clean", ExerciseCategory.FULL_BODY, Equipment.KETTLEBELL, "Full body"),
        lift("Kettlebell snatch", ExerciseCategory.FULL_BODY, Equipment.KETTLEBELL, "Full body"),
        lift("Turkish get-up", ExerciseCategory.FULL_BODY, Equipment.KETTLEBELL, "Full body"),
        lift("Devil press", ExerciseCategory.FULL_BODY, Equipment.DUMBBELL, "Full body"),
        lift("Man maker", ExerciseCategory.FULL_BODY, Equipment.DUMBBELL, "Full body"),
        lift("Medicine ball slam", ExerciseCategory.FULL_BODY, Equipment.OTHER, "Full body"),
        lift("Wall ball", ExerciseCategory.FULL_BODY, Equipment.OTHER, "Full body"),
        lift("Hex bar jump", ExerciseCategory.FULL_BODY, Equipment.BARBELL, "Legs"),
        body("Burpee", ExerciseCategory.FULL_BODY, "Full body"),
        body("Box jump", ExerciseCategory.FULL_BODY, "Legs"),
        body("Broad jump", ExerciseCategory.FULL_BODY, "Legs"),
        body("Jump squat", ExerciseCategory.FULL_BODY, "Legs"),
        Ex("Farmer's walk", ExerciseCategory.FULL_BODY, Equipment.DUMBBELL, "Grip", weight = true, reps = false, duration = true, distance = true),
        Ex("Suitcase carry", ExerciseCategory.FULL_BODY, Equipment.DUMBBELL, "Obliques", weight = true, reps = false, duration = true, distance = true),
        Ex("Yoke walk", ExerciseCategory.FULL_BODY, Equipment.OTHER, "Full body", weight = true, reps = false, duration = true, distance = true),
        Ex("Sled push", ExerciseCategory.FULL_BODY, Equipment.OTHER, "Legs", weight = true, reps = false, duration = true, distance = true),
        Ex("Sled pull", ExerciseCategory.FULL_BODY, Equipment.OTHER, "Legs", weight = true, reps = false, duration = true, distance = true),
        hold("Battle ropes", ExerciseCategory.FULL_BODY, Equipment.OTHER, "Full body"),

        // ========================================================= cardio
        cardio("Running"),
        cardio("Treadmill running"),
        cardio("Jogging"),
        cardio("Sprint intervals"),
        cardio("Walking"),
        cardio("Treadmill walking"),
        cardio("Incline walking"),
        cardio("Hiking"),
        cardio("Cycling"),
        cardio("Stationary bike"),
        cardio("Spin class"),
        cardio("Swimming"),
        cardio("Rowing machine"),
        cardio("Elliptical"),
        cardio("Stair climber"),
        cardio("Jump rope", "Calves", distance = false),
        cardio("HIIT", distance = false),
        cardio("Boxing", "Upper body", distance = false),
        cardio("Kickboxing", distance = false),
        cardio("Yoga", distance = false),
        cardio("Surya namaskar", distance = false),
        cardio("Pilates", "Core", distance = false),
        cardio("Stretching", distance = false),
        cardio("Foam rolling", distance = false),
        cardio("Dancing", distance = false),
        cardio("Zumba", distance = false),
        cardio("Cricket", distance = false),
        cardio("Football", distance = false),
        cardio("Badminton", distance = false),
        cardio("Tennis", distance = false),
        cardio("Table tennis", distance = false),
        cardio("Basketball", distance = false),
        cardio("Volleyball", distance = false),
        cardio("Squash", distance = false),
        cardio("Kabaddi", distance = false),
        cardio("Martial arts", distance = false)
    )

    fun entities(now: Long = System.currentTimeMillis()): List<ExerciseEntity> =
        EXERCISES.map { e ->
            ExerciseEntity(
                name = e.name,
                category = e.category.id,
                equipment = e.equipment.id,
                primaryMuscle = e.muscle,
                tracksWeight = e.weight,
                tracksReps = e.reps,
                tracksDuration = e.duration,
                tracksDistance = e.distance,
                isCustom = false,
                createdAt = now
            )
        }

    val count: Int get() = EXERCISES.size
}
