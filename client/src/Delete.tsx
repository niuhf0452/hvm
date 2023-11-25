import { DeleteOutline } from "@suid/icons-material";
import { Button, IconButton, Modal, Paper } from "@suid/material";
import { toast } from "solid-toast";
import fetch from "./fetch";
import { MediaFile } from "./Types";
import useModal from "./Modal";
import "./Delete.scss";

export interface DeleteProps {
  file?: MediaFile;
  onDeleted?: () => void;
}

export default function Delete(props: DeleteProps) {
  const context = useModal();

  const onClick = () => {
    if (props.file) {
      context.open();
    }
  };

  const onDelete = async (close: () => void) => {
    const path = props.file?.path;
    if (!path) return;
    const file = encodeURIComponent(path);
    const response = await fetch(`/api/media/delete?file=${file}`, {
      method: "POST",
    });
    if (response.status == 200) {
      toast.success("File deleted successfully!", {
        duration: 3000,
        position: "top-center",
      });
      close();
      props.onDeleted?.();
    } else {
      toast.error("Failed to delete file!", {
        duration: 3000,
        position: "top-center",
      });
    }
  };

  const filename = () => {
    const path = props.file?.path;
    if (!path) return;
    const name = path.substring(path.lastIndexOf("/") + 1);
    if (name.startsWith(".")) {
      return name;
    }
    const nameWithoutExt = name.substring(0, name.lastIndexOf("."));
    return nameWithoutExt;
  };

  return (
    <>
      <IconButton onClick={onClick}>
        <DeleteOutline />
      </IconButton>
      <Modal open={context.isOpen()}>
        <Paper class="delete__paper">
          <div class="delete__title">Delete File</div>
          <p>Are you sure to delete file?</p>
          <p>{filename()}</p>
          <div class="delete__buttons">
            <Button onClick={() => onDelete(context.close)}>DELETE</Button>
            <Button onClick={context.close}>Cancel</Button>
          </div>
        </Paper>
      </Modal>
    </>
  );
}
