import { createSignal } from "solid-js";
import { DriveFileRenameOutline } from "@suid/icons-material";
import { Button, IconButton, Modal, Paper, TextField } from "@suid/material";
import { toast } from "solid-toast";
import { MediaFile } from "./Types";
import "./Rename.scss";

export interface RenameProps {
  file?: MediaFile;
}

export default function Rename(props: RenameProps) {
  const [open, setOpen] = createSignal(false);
  const [newName, setNewName] = createSignal("");
  let textField: HTMLElement | null = null;

  const onOpen = () => {
    if (!props.file) return;
    setOpen(true);
    textField?.querySelector("input")?.focus();
    window.addEventListener("keyup", onKeyUp);
  };
  const onClose = () => {
    setOpen(false);
    window.removeEventListener("keyup", onKeyUp);
  };
  const onChange = (e: Event) => {
    const input = e.target as HTMLInputElement;
    const value = input.value;
    setNewName(value);
  };
  const onRename = async () => {
    const path = props.file?.path;
    if (!path) return;
    const dir = path.substring(0, path.lastIndexOf("/"));
    const ext = path.substring(path.lastIndexOf("."));
    const to = encodeURIComponent(dir + "/" + newName() + ext);
    const from = encodeURIComponent(path);
    const response = await fetch(`/api/media/move?from=${from}&to=${to}`, {
      method: "POST",
    });
    if (response.status == 200) {
      toast.success("File renamed successfully!");
    } else {
      toast.error("Failed to rename file!");
    }
  };

  const filename = () => {
    const path = props.file?.path;
    if (!path) return;
    const name = path.substring(path.lastIndexOf("/") + 1);
    const nameWithoutExt = name.substring(0, name.lastIndexOf("."));
    return nameWithoutExt;
  };

  const onKeyUp = (e: KeyboardEvent) => {
    if (e.key == "Escape") {
      onClose();
    }
  };

  return (
    <>
      <IconButton onClick={onOpen}>
        <DriveFileRenameOutline />
      </IconButton>
      <Modal open={open()}>
        <Paper class="rename__paper">
          <div class="rename__title">Rename File</div>
          <p>Please input the new file name:</p>
          <TextField
            class="rename__input"
            placeholder={filename()}
            hiddenLabel
            size="small"
            ref={(el) => (textField = el)}
            onChange={onChange}
          />
          <div class="rename__buttons">
            <Button onClick={onRename}>Rename</Button>
            <Button onClick={onClose}>Close</Button>
          </div>
        </Paper>
      </Modal>
    </>
  );
}
