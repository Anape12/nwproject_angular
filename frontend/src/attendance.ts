import { Component, computed, ElementRef, inject, signal } from "@angular/core";

interface AttendanceRow {
    id: string;
    date: string;
    workType: string;
    workTypeLabel: string;
    attendanceType: string | null;
    clockIn: string;
    clockOut: string;
    breakMinutes: number;
    workingTime: string;
    reportId: string;
    reportTitle: string | null;
    approvalStatus: string | null;
    approvalStatusLabel: string;
    editable: boolean;
    note: string | null;
}

interface DayRow {
    date: string;
    number: number;
    weekday: string;
    today: boolean;
    weekend: boolean;
    attendance: AttendanceRow | null;
}

@Component({
    selector: "nw-attendance",
    templateUrl: "./attendance.html",
    host: { style: "display: contents" },
})
export class AttendanceComponent {
    readonly data = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset;
    readonly contextPath = this.data["contextPath"] ?? "";
    readonly action = this.contextPath + "/WorkManagement";
    readonly attendanceAction = this.contextPath + "/AttendanceAction";
    readonly reportAction = this.contextPath + "/DairyWrite";
    readonly currentUrl = this.monthUrl(this.data["year"], this.data["month"]);
    readonly previousUrl = this.monthUrl(this.data["previousYear"], this.data["previousMonth"]);
    readonly nextUrl = this.monthUrl(this.data["nextYear"], this.data["nextMonth"]);
    readonly closed = this.data["closed"] === "true";
    readonly days: DayRow[] = JSON.parse(this.data["days"] || "[]");
    readonly summary: { days?: number; leave?: number; absent?: number; overtime?: number } =
        JSON.parse(this.data["summary"] || "{}");
    readonly selected: AttendanceRow | null = JSON.parse(this.data["selected"] || "null");
    private readonly reports: { id: string; date: string; title: string; statusLabel: string }[] =
        JSON.parse(this.data["reports"] || "[]");
    readonly workDate = signal(this.selected?.date ?? this.data["selectedDate"] ?? "");
    readonly workType = signal(this.selected?.workType ?? "OFFICE");
    readonly nonWorking = computed(() => this.workType() === "LEAVE" || this.workType() === "HOLIDAY");
    readonly matchingReports = computed(() => this.reports.filter(report => report.date === this.workDate()));
    readonly reportId = signal(this.matchingReports().some(report => report.id === this.selected?.reportId)
        ? this.selected?.reportId ?? "" : "");
    readonly workTypes = [
        { id: "OFFICE", label: "出社" }, { id: "REMOTE", label: "リモート" },
        { id: "LEAVE", label: "休暇" }, { id: "HOLIDAY", label: "休日" },
    ];
    readonly attendanceTypes = [
        { id: "NORMAL", label: "通常" }, { id: "LATE", label: "遅刻" }, { id: "EARLY", label: "早退" },
        { id: "ABSENT", label: "欠勤" }, { id: "PAID_LEAVE", label: "有給休暇" },
        { id: "COMP_LEAVE", label: "振替休日" }, { id: "HOLIDAY_WORK", label: "休日出勤" },
    ];

    changeDate(date: string): void {
        this.workDate.set(date);
        if (!this.matchingReports().some(report => report.id === this.reportId())) this.reportId.set("");
    }

    confirm(event: Event, message: string): void {
        if (!window.confirm(message)) event.preventDefault();
    }

    private monthUrl(year = "", month = ""): string {
        return this.action + "?" + new URLSearchParams({ year, month });
    }
}
