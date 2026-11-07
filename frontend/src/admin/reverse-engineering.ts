import { Component, computed, ElementRef, inject, OnInit, signal } from "@angular/core";

type View = "database" | "java";
interface Table {
    name: string;
    columns: {
        name: string; type: string; size: number; nullable: boolean; primaryKey: boolean;
        defaultValue: string | null; remarks: string | null;
    }[];
}
interface JavaClass {
    name: string;
    simpleName: string;
    packageName: string;
    kind: string;
    fields: { name: string; type: string }[];
    methods: string[];
}
interface Database {
    tables: Table[];
    relations: { fromTable: string; toTable: string; fromColumn: string; toColumn: string }[];
}
interface JavaStructure {
    classes: JavaClass[];
    relations: { from: string; to: string; kind: string }[];
}

@Component({
    selector: "nw-reverse-engineering",
    templateUrl: "./reverse-engineering.html",
    host: { style: "display: contents" },
})
export class ReverseEngineeringComponent implements OnInit {
    readonly contextPath = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.dataset["contextPath"] ?? "";
    readonly view = signal<View>("database");
    readonly search = signal("");
    readonly selectedName = signal<string | null>(null);
    readonly loading = signal(true);
    readonly error = signal("");
    private readonly database = signal<Database | null>(null);
    private readonly java = signal<JavaStructure | null>(null);
    private requestId = 0;

    readonly items = computed(() => this.view() === "database" ? this.database()?.tables ?? [] : this.java()?.classes ?? []);
    readonly filtered = computed(() => {
        const term = this.search().trim().toLocaleLowerCase();
        return this.items().filter(item =>
            item.name.toLocaleLowerCase().includes(term) || ("columns" in item
                ? item.columns.some(column => column.name.toLocaleLowerCase().includes(term))
                : item.packageName.toLocaleLowerCase().includes(term)));
    });
    readonly selected = computed(() =>
        this.filtered().find(item => item.name === this.selectedName()) ?? this.filtered().at(0));
    readonly selectedTable = computed(() => {
        const item = this.selected();
        return item && "columns" in item ? item : null;
    });
    readonly selectedClass = computed(() => {
        const item = this.selected();
        return item && "simpleName" in item ? item : null;
    });
    readonly relations = computed(() => {
        const name = this.selected()?.name;
        if (!name) return [];
        if (this.view() === "database") {
            return (this.database()?.relations ?? [])
                .filter(rel => rel.fromTable === name || rel.toTable === name)
                .map(rel => ({
                    neighbour: rel.fromTable === name ? rel.toTable : rel.fromTable,
                    label: `${rel.fromTable}.${rel.fromColumn} → ${rel.toTable}.${rel.toColumn} (FK)`,
                }));
        }
        return (this.java()?.relations ?? [])
            .filter(rel => rel.from === name || rel.to === name)
            .map(rel => ({
                neighbour: rel.from === name ? rel.to : rel.from,
                label: `${this.shortName(rel.from)} → ${this.shortName(rel.to)} (${rel.kind})`,
            }));
    });
    readonly neighbours = computed(() => [...new Set(this.relations().map(rel => rel.neighbour))].slice(0, 8));
    readonly height = computed(() => this.neighbours().length ? Math.max(250, this.neighbours().length * 58 + 40) : 200);

    ngOnInit(): void {
        void this.load("database");
    }

    async load(view: View): Promise<void> {
        const requestId = ++this.requestId;
        this.view.set(view);
        this.search.set("");
        this.selectedName.set(null);
        this.error.set("");
        const cached = view === "database" ? this.database() : this.java();
        this.loading.set(!cached);
        if (cached) return;

        try {
            const response = await fetch(this.contextPath + "/ReverseEngineering?view=" + view, { mode: "same-origin" });
            if (requestId !== this.requestId) return;
            if (response.redirected) {
                location.assign(response.url);
                return;
            }
            if (!response.ok) throw new Error("HTTP " + response.status);
            const structure = await response.json();
            if (requestId !== this.requestId) return;
            if (view === "database") this.database.set(structure);
            else this.java.set(structure);
        } catch (error) {
            if (requestId === this.requestId) {
                const reason = error instanceof Error ? error.message : "";
                this.error.set(`構造を取得できませんでした（${reason}）。再読み込みしてください。`);
            }
        } finally {
            if (requestId === this.requestId) this.loading.set(false);
        }
    }

    updateSearch(value: string): void {
        const selected = this.selected()?.name ?? null;
        this.search.set(value);
        const filtered = this.filtered();
        this.selectedName.set(filtered.some(item => item.name === selected) ? selected : filtered[0]?.name ?? null);
    }

    shortName(name: string): string {
        return this.view() === "java" ? name.substring(name.lastIndexOf(".") + 1) : name;
    }

    diagramLabel(name: string): string {
        const label = this.shortName(name);
        return label.length > 33 ? label.substring(0, 30) + "…" : label;
    }

    download(): void {
        const view = this.view();
        const structure = view === "database" ? this.database() : this.java();
        if (!structure) return;
        const url = URL.createObjectURL(new Blob([JSON.stringify(structure, null, 2)], { type: "application/json;charset=utf-8" }));
        const link = document.createElement("a");
        link.href = url;
        link.download = "nwproject-" + view + "-reverse.json";
        link.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
    }
}
