import { For, Match, Switch, createComputed, createSignal } from "solid-js";
import { createFetch } from "@solid-primitives/fetch";
import { createPolled } from "@solid-primitives/timer";
import { List as ListIcon } from "@suid/icons-material";
import {
  Button,
  IconButton,
  Modal,
  Paper,
  List,
  ListItemButton,
  ListItemText,
  CircularProgress,
} from "@suid/material";
import "./Tasks.scss";

type Task = {
  id: number;
  name: string;
  progress: number; // unit 1/1000
  output: string;
};

export default function Tasks() {
  const [open, setOpen] = createSignal(false);
  const [selected, setSelected] = createSignal(0);
  const [tasks, { refetch }] = createFetch<Task[]>("/api/tasks");

  createPolled(() => refetch(), 5000);
  createComputed(() => {
    setSelected((id) => {
      const ts = tasks();
      if (!ts || ts.length === 0) return 0;
      const t = ts.find((t) => t.id === id);
      return t ? id : ts[0].id;
    });
    return tasks();
  });

  const onOpen = () => {
    setOpen(true);
    window.addEventListener("keyup", onKeyUp);
  };
  const onClose = () => {
    setOpen(false);
    window.removeEventListener("keyup", onKeyUp);
  };
  const onKeyUp = (e: KeyboardEvent) => {
    if (e.key == "Escape") {
      onClose();
    }
  };
  const onSelect = (task: Task) => () => {
    setSelected(task.id);
  };
  const output = () => {
    const ts = tasks();
    if (!ts) return;
    const id = selected();
    const t = ts.find((t) => t.id === id);
    return t && t.output;
  };

  return (
    <>
      <IconButton onClick={onOpen}>
        <ListIcon />
      </IconButton>
      <Modal open={open()}>
        <Paper class="tasks__paper">
          <div class="tasks__title">Tasks</div>
          <div class="tasks__main">
            <div class="tasks__list">
              <Switch>
                <Match when={tasks.loading}>
                  <div class="tasks__list--center">
                    <CircularProgress />
                  </div>
                </Match>
                <Match when={tasks.error}>
                  <div class="tasks__list--center">
                    <span>Error</span>
                  </div>
                </Match>
                <Match when={true}>
                  <List>
                    <For each={tasks()}>
                      {(task) => (
                        <ListItemButton
                          class="tasks__item-button"
                          selected={task.id == selected()}
                          onClick={onSelect(task)}
                        >
                          <ListItemText class="tasks__item-name">
                            {task.name}
                          </ListItemText>
                        </ListItemButton>
                      )}
                    </For>
                  </List>
                </Match>
              </Switch>
            </div>
            <pre class="tasks__output">{output()}</pre>
          </div>
          <div class="tasks__buttons">
            <Button onClick={onClose}>Close</Button>
          </div>
        </Paper>
      </Modal>
    </>
  );
}
