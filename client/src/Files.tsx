import { For, Match, Show, Switch, createSignal } from "solid-js";
import {
  Paper,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Divider,
  IconButton,
  CircularProgress,
} from "@suid/material";
import { Refresh, Folder, VideoFile, ArrowUpward } from "@suid/icons-material";
import { createFetch } from "@solid-primitives/fetch";
import { MediaFile } from "./Types";
import Rename from "./Rename";
import Tasks from "./Tasks";
import "./Files.scss";

export interface FilesProps {
  onSelect?: (file: MediaFile) => void;
}

export default function Files(props: FilesProps) {
  const [path, setPath] = createSignal("/");
  const [file, setFile] = createSignal<MediaFile | undefined>(undefined);
  const [files, { refetch }] = createFetch<MediaFile[]>(
    () => `/api/media/browse?path=${encodeURIComponent(path())}`,
  );

  const onRefresh = () => {
    refetch();
  };

  const onFileClick = (file: MediaFile) => () => {
    if (file.isDirectory) {
      setPath(file.path);
    } else {
      setFile(file);
      props.onSelect?.(file);
    }
  };

  const onParentClick = () => {
    setPath((path) => {
      if (path == "/") {
        return path;
      }
      const newPath = path.split("/").slice(0, -1).join("/");
      return newPath || "/";
    });
  };

  const displaySize = (bytes: number) => {
    if (bytes > 1000000000) {
      return `${(bytes / 1000000000).toFixed(1)} GB`;
    }
    if (bytes > 1000000) {
      return `${(bytes / 1000000).toFixed(1)} MB`;
    }
    if (bytes > 1000) {
      return `${(bytes / 1000).toFixed(1)} KB`;
    }
    return `${bytes} B`;
  };

  return (
    <Paper class="files">
      <div class="files__list">
        <Switch>
          <Match when={files.loading}>
            <div class="files__list--center">
              <CircularProgress />
            </div>
          </Match>
          <Match when={files.error}>
            <div class="files__list--center">
              <span>Error</span>
            </div>
          </Match>
          <Match when={true}>
            <List>
              <Show when={path() !== "/"}>
                <ListItem disablePadding>
                  <ListItemButton onClick={onParentClick}>
                    <ListItemIcon>
                      <ArrowUpward />
                    </ListItemIcon>
                    <ListItemText>..</ListItemText>
                  </ListItemButton>
                </ListItem>
              </Show>
              <For each={files()}>
                {(file) => (
                  <ListItem disablePadding>
                    <ListItemButton
                      class="files__item-button"
                      onClick={onFileClick(file)}
                    >
                      <ListItemIcon>
                        {file.isDirectory ? <Folder /> : <VideoFile />}
                      </ListItemIcon>
                      <ListItemText class="files__name">
                        {file.name}
                      </ListItemText>
                      {!file.isDirectory && (
                        <span class="files__size">
                          {displaySize(file.size)}
                        </span>
                      )}
                    </ListItemButton>
                  </ListItem>
                )}
              </For>
            </List>
          </Match>
        </Switch>
      </div>
      <Divider />
      <div class="files__buttons">
        <IconButton onClick={onRefresh}>
          <Refresh />
        </IconButton>
        <Rename file={file()} />
        <Tasks />
      </div>
    </Paper>
  );
}
