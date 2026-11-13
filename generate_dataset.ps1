$ErrorActionPreference = "Stop"

$jsonRaw = [System.IO.File]::ReadAllText("exercises-dataset\data\exercises.json", [System.Text.Encoding]::UTF8)
$items = $jsonRaw | ConvertFrom-Json

function Escape-Kt([string]$s) {
    if ([string]::IsNullOrEmpty($s)) { return "" }
    return $s.Replace("\", "\\").Replace("$", "\$").Replace('"', '\"').Replace("`r", "").Replace("`n", " ")
}

function To-TitleCase([string]$s) {
    if ([string]::IsNullOrEmpty($s)) { return "" }
    $words = $s.Split(" ", [System.StringSplitOptions]::RemoveEmptyEntries)
    $titled = foreach ($w in $words) {
        if ($w.Length -eq 1) { $w.ToUpper() }
        else { $w.Substring(0,1).ToUpper() + $w.Substring(1).ToLower() }
    }
    return [string]::Join(" ", $titled)
}

function Map-BodyPart([string]$bp) {
    switch ($bp.ToLower().Trim()) {
        "chest" { "MuscleGroup.CHEST" }
        "back" { "MuscleGroup.BACK" }
        "upper legs" { "MuscleGroup.LEGS" }
        "lower legs" { "MuscleGroup.LEGS" }
        "shoulders" { "MuscleGroup.SHOULDERS" }
        "upper arms" { "MuscleGroup.ARMS" }
        "lower arms" { "MuscleGroup.ARMS" }
        "waist" { "MuscleGroup.CORE" }
        "cardio" { "MuscleGroup.CARDIO" }
        "neck" { "MuscleGroup.NECK" }
        default { "MuscleGroup.FULL_BODY" }
    }
}

function Map-SecondaryGroup([string]$m) {
    $s = $m.ToLower().Trim()
    if ($s -match "chest|pectoral") { return "MuscleGroup.CHEST" }
    if ($s -match "back|lat|trap|spine") { return "MuscleGroup.BACK" }
    if ($s -match "leg|glute|hamstring|quad|calv") { return "MuscleGroup.LEGS" }
    if ($s -match "shoulder|delt") { return "MuscleGroup.SHOULDERS" }
    if ($s -match "bicep|tricep|forearm|arm") { return "MuscleGroup.ARMS" }
    if ($s -match "ab|waist|core|oblique") { return "MuscleGroup.CORE" }
    if ($s -match "cardio") { return "MuscleGroup.CARDIO" }
    if ($s -match "neck") { return "MuscleGroup.NECK" }
    return $null
}

function Map-Equipment([string]$eq) {
    switch ($eq.ToLower().Trim()) {
        "barbell" { "Equipment.BARBELL" }
        "ez barbell" { "Equipment.BARBELL" }
        "olympic barbell" { "Equipment.BARBELL" }
        "trap bar" { "Equipment.BARBELL" }
        "dumbbell" { "Equipment.DUMBBELL" }
        "cable" { "Equipment.CABLE" }
        "rope" { "Equipment.CABLE" }
        "body weight" { "Equipment.BODYWEIGHT" }
        "assisted" { "Equipment.BODYWEIGHT" }
        "weighted" { "Equipment.BODYWEIGHT" }
        "smith machine" { "Equipment.SMITH_MACHINE" }
        "band" { "Equipment.BAND" }
        "resistance band" { "Equipment.BAND" }
        "kettlebell" { "Equipment.KETTLEBELL" }
        "leverage machine" { "Equipment.MACHINE" }
        "sled machine" { "Equipment.MACHINE" }
        "stationary bike" { "Equipment.MACHINE" }
        "elliptical machine" { "Equipment.MACHINE" }
        "stepmill machine" { "Equipment.MACHINE" }
        "skierg machine" { "Equipment.MACHINE" }
        "hammer" { "Equipment.MACHINE" }
        "upper body ergometer" { "Equipment.MACHINE" }
        default { "Equipment.OTHER" }
    }
}

function Get-MovementType($ex) {
    $name = $ex.name.ToLower().Trim()
    $target = $ex.target.ToLower().Trim()
    $bodyPart = $ex.body_part.ToLower().Trim()
    $secCount = if ($ex.secondary_muscles) { $ex.secondary_muscles.Count } else { 0 }

    # Major compound indicators (multi-joint movements)
    if ($name -match "squat|press|deadlift|clean|snatch|row|pull-up|chin-up|dip|lunge|thruster|burpee|step-up|push-up|pushup") {
        if ($name -notmatch "pressdown|wrist") {
            return "MovementType.COMPOUND"
        }
    }

    # Major isolation indicators (single joint motions)
    if ($name -match "curl|extension|fly|raise|lateral|shrug|crunch|kickback|pushdown|wrist|abduction|adduction|side bend|calf|twist|sit-up|pullover|hyper|hyperextension") {
        return "MovementType.ISOLATION"
    }

    # If body part is arms or lower legs and few secondary muscles, it's isolation
    if (($bodyPart -match "arm|lower leg") -and ($secCount -le 1)) {
        return "MovementType.ISOLATION"
    }

    # If target is specific isolation target
    if ($target -match "biceps|triceps|forearms|calves|brachialis" -and ($secCount -le 1)) {
        return "MovementType.ISOLATION"
    }

    # Core exercises
    if ($bodyPart -eq "waist") {
        if ($name -match "crunch|twist|bend|rollout|sit-up|toucher|bridge|flutter|scissor|hollow") {
            return "MovementType.ISOLATION"
        }
    }

    if ($secCount -ge 2) {
        return "MovementType.COMPOUND"
    }

    return "MovementType.COMPOUND"
}

$sb = [System.Text.StringBuilder]::new()
[void]$sb.AppendLine("// Code generated from exercises-dataset/data/exercises.json. DO NOT EDIT DIRECTLY.")
[void]$sb.AppendLine("package com.bearbones.kumalift.data.local")
[void]$sb.AppendLine()
[void]$sb.AppendLine("import com.bearbones.kumalift.domain.model.Equipment")
[void]$sb.AppendLine("import com.bearbones.kumalift.domain.model.Exercise")
[void]$sb.AppendLine("import com.bearbones.kumalift.domain.model.ExperienceLevel")
[void]$sb.AppendLine("import com.bearbones.kumalift.domain.model.MovementType")
[void]$sb.AppendLine("import com.bearbones.kumalift.domain.model.MuscleGroup")
[void]$sb.AppendLine()
[void]$sb.AppendLine("object ExerciseDataset {")

$chunkSize = 135
$totalChunks = [Math]::Ceiling($items.Count / $chunkSize)

for ($c = 0; $c -lt $totalChunks; $c++) {
    [void]$sb.AppendLine("    private fun chunk$c(): List<Exercise> = listOf(")
    $startIdx = $c * $chunkSize
    $endIdx = [Math]::Min($startIdx + $chunkSize - 1, $items.Count - 1)

    for ($i = $startIdx; $i -le $endIdx; $i++) {
        $ex = $items[$i]
        $id = $ex.id
        $name = Escape-Kt (To-TitleCase $ex.name)
        $primaryMuscle = Map-BodyPart $ex.body_part
        $subMuscle = Escape-Kt $ex.target
        
        $secMusclesArr = [System.Collections.Generic.List[string]]::new()
        $secDetailsList = [System.Collections.Generic.List[string]]::new()
        if ($ex.secondary_muscles) {
            foreach ($sm in $ex.secondary_muscles) {
                $secDetailsList.Add($sm)
                $grp = Map-SecondaryGroup $sm
                if ($grp -and !$secMusclesArr.Contains($grp)) {
                    $secMusclesArr.Add($grp)
                }
            }
        }
        $secMusclesCode = if ($secMusclesArr.Count -gt 0) { "listOf(" + ($secMusclesArr -join ", ") + ")" } else { "emptyList()" }
        $secDetailsStr = Escape-Kt ($secDetailsList -join ", ")
        $equipmentCode = Map-Equipment $ex.equipment
        $eqDetail = Escape-Kt $ex.equipment

        $instr = Escape-Kt $ex.instructions.en
        $stepsCode = if ($ex.instruction_steps -and $ex.instruction_steps.en -and $ex.instruction_steps.en.Count -gt 0) {
            $stepsEscaped = $ex.instruction_steps.en | ForEach-Object { "`"" + (Escape-Kt $_) + "`"" }
            "listOf(" + ($stepsEscaped -join ", ") + ")"
        } else {
            "emptyList()"
        }

        $imagePath = Escape-Kt $ex.image
        $gifPath = Escape-Kt $ex.gif_url
        $attributionStr = "© Gym visual"
        $movementTypeCode = Get-MovementType $ex

        $prCode = ""
        if ($id -eq "0025") {
            $prCode = "currentPrWeightKg = 118.0, currentPrReps = 3, lastLoggedDate = `"2 hari lalu`","
        }

        [void]$sb.AppendLine("        Exercise(")
        [void]$sb.AppendLine("            id = `"$id`",")
        [void]$sb.AppendLine("            name = `"$name`",")
        [void]$sb.AppendLine("            primaryMuscle = $primaryMuscle,")
        [void]$sb.AppendLine("            subMuscle = `"$subMuscle`",")
        [void]$sb.AppendLine("            secondaryMuscles = $secMusclesCode,")
        [void]$sb.AppendLine("            secondaryMuscleDetails = `"$secDetailsStr`",")
        [void]$sb.AppendLine("            equipment = $equipmentCode,")
        [void]$sb.AppendLine("            equipmentDetail = `"$eqDetail`",")
        [void]$sb.AppendLine("            movementType = $movementTypeCode,")
        [void]$sb.AppendLine("            level = ExperienceLevel.BEGINNER,")
        [void]$sb.AppendLine("            instructions = `"$instr`",")
        [void]$sb.AppendLine("            instructionSteps = $stepsCode,")
        [void]$sb.AppendLine("            gifUrl = `"$gifPath`",")
        [void]$sb.AppendLine("            thumbnailUrl = `"$imagePath`",")
        [void]$sb.AppendLine("            attribution = `"$attributionStr`",")
        if ($prCode) {
            [void]$sb.AppendLine("            $prCode")
        }
        [void]$sb.AppendLine("            defaultIncrementKg = 2.5")
        [void]$sb.AppendLine("        ),")
    }
    [void]$sb.AppendLine("    )")
    [void]$sb.AppendLine()
}

$chunkNames = (0..($totalChunks - 1) | ForEach-Object { "chunk$_()" }) -join " + "
[void]$sb.AppendLine("    val allExercises: List<Exercise> by lazy {")
[void]$sb.AppendLine("        $chunkNames")
[void]$sb.AppendLine("    }")
[void]$sb.AppendLine("}")

$destPath = "composeApp\src\commonMain\kotlin\com\bearbones\kumalift\data\local\ExerciseDataset.kt"
[System.IO.File]::WriteAllText($destPath, $sb.ToString(), [System.Text.Encoding]::UTF8)
Write-Host "Generated ExerciseDataset.kt with $($items.Count) exercises across $totalChunks chunks."
