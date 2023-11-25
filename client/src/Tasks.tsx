import { For, createComputed } from "solid-js";
import { List as ListIcon } from "@suid/icons-material";
import {
  Button,
  IconButton,
  Modal,
  Paper,
  LinearProgress,
} from "@suid/material";
import toast from "solid-toast";
import { useSSE } from "./sse";
import useModal from "./Modal";
import { Task, TaskState } from "./Types";
import "./Tasks.scss";

type TaskId = number;
type LoadingState = {
  toastId: string | undefined;
  errors: TaskId[];
};

export default function Tasks() {
  const { value: tasks } = useSSE<Task[]>({
    url: "/api/tasks",
    defaultValue: [],
    onMessage: (tasks, arr) =>
      arr.reduce(
        (a, b) => (b.type === "data" ? (JSON.parse(b.value) as Task[]) : a),
        tasks
      ),
  });
  const { isOpen, open, close } = useModal();

  createComputed<LoadingState>(
    (state) => {
      let loading = false;
      let errors = state.errors;
      let toastId = state.toastId;
      tasks().forEach((task) => {
        if (
          task.state === TaskState.Pending ||
          task.state === TaskState.Running
        ) {
          loading = true;
        }
        if (
          task.state === TaskState.Error &&
          state.errors.indexOf(task.id) < 0
        ) {
          errors = [...state.errors, task.id];
          toast.error(`Error while running task: ${task.error}`, {
            duration: 5000,
            position: "top-right",
          });
        }
      });
      if (loading && !toastId) {
        toastId = toast.loading("Running tasks ...", {
          position: "top-right",
        });
      } else if (!loading && toastId) {
        toast.dismiss(toastId);
      }
      return { toastId, errors };
    },
    { toastId: undefined, errors: [] }
  );

  const onClear = () => {
    fetch("/api/tasks/clear", { method: "POST" });
  };

  return (
    <>
      <IconButton onClick={open}>
        <ListIcon />
      </IconButton>
      <Modal open={isOpen()}>
        <Paper class="tasks__paper">
          <div class="tasks__title">Tasks</div>
          <div class="tasks__list">
            <For each={tasks()}>
              {(task) => (
                <div class="tasks__item">
                  <div class="tasks__item-name">{task.name}</div>
                  <div class="tasks__item-progress">
                    <LinearProgress
                      variant="determinate"
                      value={task.percent}
                    />
                  </div>
                  <div class="tasks__item-percent">{task.percent}%</div>
                </div>
              )}
            </For>
          </div>
          <div class="tasks__buttons">
            <Button onClick={onClear}>Clear</Button>
            <Button onClick={close}>Close</Button>
          </div>
        </Paper>
      </Modal>
    </>
  );
}
