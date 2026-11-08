import { Component, ElementRef, inject, signal } from "@angular/core";
import { projectRequest } from "../infr/project-request";

interface CalendarDay {
    date: string;
    number: number;
    currentMonth: boolean;
    today: boolean;
    events: { id: string; title: string; color: string; time: string }[];
}

interface EditEvent {
    id: string;
    title: string;
    allDay: boolean;
    startDate: string;
    endDate: string;
    startTime: string;
    endTime: string;
    color: string;
    visibility: string;
    recurrence: string | null;
    recurrenceUntil: string;
    description: string | null;
}

@Component({
    selector: "nw-schedule-calendar",
    templateUrl: "./schedule-calendar.html",
    host: { style: "display: contents" },
})
export class ScheduleCalendarComponent {
    readonly project = projectRequest;
    readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly action = projectRequest.url("calendar");
    readonly currentUrl = this.monthUrl(this.data["year"], this.data["month"]);
    readonly previousUrl = this.monthUrl(this.data["previousYear"], this.data["previousMonth"]);
    readonly nextUrl = this.monthUrl(this.data["nextYear"], this.data["nextMonth"]);
    readonly days: CalendarDay[] = JSON.parse(this.data["days"] || "[]");
    readonly event: EditEvent | null = JSON.parse(this.data["event"] || "null");
    readonly users: { id: string; name: string }[] = JSON.parse(this.data["users"] || "[]");
    readonly attachments: { id: string; name: string }[] = JSON.parse(this.data["attachments"] || "[]");
    readonly allDay = signal(this.event?.allDay ?? false);
    readonly colors = [
        { id: "#1a73e8", label: "ブルー" }, { id: "#188038", label: "グリーン" },
        { id: "#d93025", label: "レッド" }, { id: "#9334e6", label: "パープル" },
        { id: "#f9ab00", label: "オレンジ" }, { id: "#5f6368", label: "グレー" },
    ];

    constructor() {
        document.title = `${this.data.year}年${this.data.month}月 スケジュール`;
    }

    private monthUrl(year = "", month = ""): string {
        return this.action + "?" + new URLSearchParams({ year, month });
    }

    confirmDelete(event: Event): void {
        if (!window.confirm("この予定を削除しますか？")) event.preventDefault();
    }
}
