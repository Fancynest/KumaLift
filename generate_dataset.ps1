$ErrorActionPreference = "Stop"

$jsonRaw = [System.IO.File]::ReadAllText("exercises-dataset\data\exercises.json", [System.Text.Encoding]::UTF8)
$items = $jsonRaw | ConvertFrom-Json

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
        "chest" { "CHEST" }
        "back" { "BACK" }
        "upper legs" { "LEGS" }
        "lower legs" { "LEGS" }
        "shoulders" { "SHOULDERS" }
        "upper arms" { "ARMS" }
        "lower arms" { "ARMS" }
        "waist" { "CORE" }
        "cardio" { "CARDIO" }
        "neck" { "NECK" }
        default { "FULL_BODY" }
    }
}

function Map-SecondaryGroup([string]$m) {
    $s = $m.ToLower().Trim()
    if ($s -match "chest|pectoral") { return "CHEST" }
    if ($s -match "back|lat|trap|spine") { return "BACK" }
    if ($s -match "leg|glute|hamstring|quad|calv") { return "LEGS" }
    if ($s -match "shoulder|delt") { return "SHOULDERS" }
    if ($s -match "bicep|tricep|forearm|arm") { return "ARMS" }
    if ($s -match "ab|waist|core|oblique") { return "CORE" }
    if ($s -match "cardio") { return "CARDIO" }
    if ($s -match "neck") { return "NECK" }
    return $null
}

function Map-Equipment([string]$eq) {
    switch ($eq.ToLower().Trim()) {
        "barbell" { "BARBELL" }
        "ez barbell" { "BARBELL" }
        "olympic barbell" { "BARBELL" }
        "trap bar" { "BARBELL" }
        "dumbbell" { "DUMBBELL" }
        "cable" { "CABLE" }
        "rope" { "CABLE" }
        "body weight" { "BODYWEIGHT" }
        "assisted" { "BODYWEIGHT" }
        "weighted" { "BODYWEIGHT" }
        "smith machine" { "SMITH_MACHINE" }
        "band" { "BAND" }
        "resistance band" { "BAND" }
        "kettlebell" { "KETTLEBELL" }
        "leverage machine" { "MACHINE" }
        "sled machine" { "MACHINE" }
        "stationary bike" { "MACHINE" }
        "elliptical machine" { "MACHINE" }
        "stepmill machine" { "MACHINE" }
        "skierg machine" { "MACHINE" }
        "hammer" { "MACHINE" }
        "upper body ergometer" { "MACHINE" }
        default { "OTHER" }
    }
}

function Get-MovementType($ex) {
    $name = $ex.name.ToLower().Trim()
    $target = $ex.target.ToLower().Trim()
    $bodyPart = $ex.body_part.ToLower().Trim()
    $secCount = if ($ex.secondary_muscles) { $ex.secondary_muscles.Count } else { 0 }

    if ($name -match "squat|press|deadlift|clean|snatch|row|pull-up|chin-up|dip|lunge|thruster|burpee|step-up|push-up|pushup") {
        if ($name -notmatch "pressdown|wrist") {
            return "COMPOUND"
        }
    }

    if ($name -match "curl|extension|fly|raise|lateral|shrug|crunch|kickback|pushdown|wrist|abduction|adduction|side bend|calf|twist|sit-up|pullover|hyper|hyperextension") {
        return "ISOLATION"
    }

    if (($bodyPart -match "arm|lower leg") -and ($secCount -le 1)) {
        return "ISOLATION"
    }

    if ($target -match "biceps|triceps|forearms|calves|brachialis" -and ($secCount -le 1)) {
        return "ISOLATION"
    }

    if ($bodyPart -eq "waist") {
        if ($name -match "crunch|twist|bend|rollout|sit-up|toucher|bridge|flutter|scissor|hollow") {
            return "ISOLATION"
        }
    }

    if ($secCount -ge 2) {
        return "COMPOUND"
    }

    return "COMPOUND"
}

$mappedList = [System.Collections.Generic.List[PSObject]]::new()

foreach ($ex in $items) {
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

    $steps = if ($ex.instruction_steps -and $ex.instruction_steps.en) { $ex.instruction_steps.en } else { @() }

    $obj = [PSCustomObject]@{
        id = $ex.id
        name = To-TitleCase $ex.name
        primaryMuscle = Map-BodyPart $ex.body_part
        subMuscle = if ($ex.target) { $ex.target } else { "" }
        secondaryMuscles = $secMusclesArr
        secondaryMuscleDetails = ($secDetailsList -join ", ")
        equipment = Map-Equipment $ex.equipment
        equipmentDetail = if ($ex.equipment) { $ex.equipment } else { "" }
        movementType = Get-MovementType $ex
        level = "BEGINNER"
        instructions = if ($ex.instructions.en) { $ex.instructions.en } else { "" }
        instructionSteps = $steps
        defaultIncrementKg = 2.5
        currentPrWeightKg = $null
        currentPrReps = $null
        lastLoggedDate = $null
        gifUrl = $ex.gif_url
        thumbnailUrl = $ex.image
        attribution = "© Gym visual"
        pinnedNote = ""
    }
    $mappedList.Add($obj)
}

$jsonOut = $mappedList | ConvertTo-Json -Depth 5 -Compress
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllText("composeApp\src\commonMain\resources\exercises.json", $jsonOut, $utf8NoBom)
Write-Host "Generated exercises.json with $($mappedList.Count) exercises successfully."
