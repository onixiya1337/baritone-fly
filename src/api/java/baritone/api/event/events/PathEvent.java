package baritone.api.event.events;

public enum PathEvent {
    CALC_STARTED,
    CALC_FINISHED_NOW_EXECUTING,
    CALC_FAILED,
    NEXT_SEGMENT_CALC_STARTED,
    NEXT_SEGMENT_CALC_FINISHED,
    CONTINUING_ONTO_PLANNED_NEXT,
    SPLICING_ONTO_NEXT_EARLY,
    AT_GOAL,
    PATH_FINISHED_NEXT_STILL_CALCULATING,
    NEXT_CALC_FAILED,
    DISCARD_NEXT,
    CANCELED;
}