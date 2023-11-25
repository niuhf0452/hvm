export type MediaFile = {
  name: string;
  path: string;
  size: number;
  isDirectory: boolean;
};

export type Task = {
  id: number;
  name: string;
  percent: number;
  error?: string;
  state: TaskState;
};

export enum TaskState {
  Pending = "pending",
  Running = "running",
  Finished = "finished",
  Error = "error",
  Canceled = "canceled",
}
