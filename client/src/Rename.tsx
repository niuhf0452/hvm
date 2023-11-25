import { createSignal } from "solid-js";
import { DriveFileRenameOutline } from "@suid/icons-material";
import { Button, IconButton, TextField, Modal, Paper } from "@suid/material";
import { toast } from "solid-toast";
import { MediaFile } from "./Types";
import useModal from "./Modal";
import fetch from "./fetch";
import "./Rename.scss";

export interface RenameProps {
  file?: MediaFile;
  onRenamed?: () => void;
}

export default function Rename(props: RenameProps) {
  const [newName, setNewName] = createSignal("");
  let textField: HTMLElement | null = null;

  const context = useModal({
    onChange: (isOpen) => {
      isOpen && textField?.focus();
    },
  });

  const onChange = (e: Event) => {
    const input = e.target as HTMLInputElement;
    const value = input.value;
    setNewName(value);
  };

  const onRename = async (close: () => void) => {
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
      toast.success("File renamed successfully!", {
        duration: 3000,
        position: "top-center",
      });
      close();
      props.onRenamed?.();
    } else {
      toast.error("Failed to rename file!", {
        duration: 3000,
        position: "top-center",
      });
    }
  };

  const filename = () => {
    const path = props.file?.path;
    if (!path) return;
    const name = path.substring(path.lastIndexOf("/") + 1);
    const nameWithoutExt = name.substring(0, name.lastIndexOf("."));
    return nameWithoutExt;
  };

  return (
    <>
      <IconButton onClick={context.open}>
        <DriveFileRenameOutline />
      </IconButton>
      <Modal open={context.isOpen()}>
        <Paper class="rename__paper">
          <div class="rename__title">Rename File</div>
          <p>Please input the new file name (without extension):</p>
          <TextField
            class="rename__input"
            placeholder={filename()}
            hiddenLabel
            size="small"
            ref={(el) => (textField = el)}
            onChange={onChange}
          />
          <div class="rename__buttons">
            <Button onClick={() => onRename(context.close)}>Rename</Button>
            <Button onClick={context.close}>Cancel</Button>
          </div>
        </Paper>
      </Modal>
    </>
  );
}
