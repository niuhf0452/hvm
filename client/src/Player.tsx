import { createSignal, createComputed, batch } from "solid-js";
import { Paper, Divider, IconButton } from "@suid/material";
import { Start, ContentCut } from "@suid/icons-material";
import { toast } from "solid-toast";
import { MediaFile } from "./Types";
import "./Player.scss";

export interface PlayerProps {
  file?: MediaFile;
}

export default function Player(props: PlayerProps) {
  const [start, setStart] = createSignal("00:00:00");
  const [end, setEnd] = createSignal("00:00:00");

  let video: HTMLVideoElement | null = null;

  const playerUrl = () => {
    const path = props.file?.path;
    if (!path) {
      return undefined;
    }
    return `/api/media/file${path}`;
  };

  const time = (seconds: number) => {
    let h = (seconds / 3600).toFixed(0).padStart(2, "0");
    let m = ((seconds % 3600) / 60).toFixed(0).padStart(2, "0");
    let s = (seconds % 60).toFixed(0).padStart(2, "0");
    return `${h}:${m}:${s}`;
  };

  const onStart = () => {
    video && setStart(time(video.currentTime));
  };
  const onEnd = () => {
    video && setEnd(time(video.currentTime));
  };

  const onCut = async () => {
    const path = props.file?.path;
    if (!path || !video || end() <= start()) {
      return;
    }
    const p = encodeURIComponent(path);
    const from = encodeURIComponent(start());
    const to = encodeURIComponent(end());
    const response = await fetch(
      `/api/media/cut?path=${p}&from=${from}&to=${to}`,
      {
        method: "POST",
      }
    );
    if (response.status == 200) {
      toast.success("Task added for cutting file!");
    } else {
      toast.error("Error while cutting file!");
    }
  };

  createComputed(() => {
    batch(() => {
      setStart("00:00:00");
      setEnd("00:00:00");
    });
    return props.file;
  });

  return (
    <Paper class="player">
      <div class="player__title">{props.file?.name}</div>
      <video
        class="player__video"
        ref={(el) => (video = el)}
        src={playerUrl()}
        controls
        autoplay
      />
      <Divider />
      <div class="player__tools">
        <IconButton onClick={onStart}>
          <Start />
        </IconButton>
        <IconButton onClick={onEnd}>
          <Start style={{ rotate: "180deg" }} />
        </IconButton>
        <IconButton onClick={onCut}>
          <ContentCut />
        </IconButton>
        <span class="player__time">{`${start()} - ${end()}`}</span>
      </div>
    </Paper>
  );
}
