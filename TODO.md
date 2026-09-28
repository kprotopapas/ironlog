# Future changes

## Per-exercise rep ranges

Right now every exercise uses the same 8–12 target (`TARGET_REPS` in
`data/Recommendation.kt`). That works for compound lifts, but on small isolation
lifts (lateral raises, rear delts, calf raises, curls, shrugs) a single 2 kg step
can be a 20–25% jump, so the go-up/go-down hints tend to swing between "hit 12"
and "under 8". A higher range such as 12–15 or 15–20 suits those exercises better.

Idea:
- Let each exercise have its own rep range, defaulting to 8–12. Library entries
  could ship sensible defaults (e.g. 12–15 for lateral raises and calf raises).
- `recommend()` already takes a `range` parameter, so the logic doesn't need to
  change; it just needs to be given the exercise's range.
- Store the range with the exercise name (not per workout), so presets, live
  workouts and the Progress tab all agree. Show it in the recommendation hint
  ("Next session · 12–15 reps").
