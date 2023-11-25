import { createSignal } from "solid-js";
import { Toaster } from "solid-toast";
import Files from "./Files";
import Player from "./Player";
import { MediaFile } from "./Types";
import "./App.scss";

function App() {
  const [file, setFile] = createSignal<MediaFile | undefined>(undefined);
  const onFileSelect = (file: MediaFile) => {
    setFile(file);
  };

  return (
    <div class="app">
      <Files onSelect={onFileSelect} />
      <Player file={file()} />
      <Toaster />
    </div>
  );
}

export default App;
