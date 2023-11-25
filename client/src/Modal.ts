import { createSignal } from "solid-js";

export interface ModalContextProps {
  onChange?: (isOpen: boolean) => void;
}

export interface ModalContext {
  open: () => void;
  close: () => void;
  isOpen: () => boolean;
}

export default function useModal(props: ModalContextProps = {}): ModalContext {
  const [isOpen, setIsOpen] = createSignal(false);

  const onOpen = () => {
    setIsOpen(true);
    window.addEventListener("keyup", onKeyUp);
    props.onChange?.(true);
  };
  const onClose = () => {
    setIsOpen(false);
    window.removeEventListener("keyup", onKeyUp);
    props.onChange?.(false);
  };
  const onKeyUp = (e: KeyboardEvent) => {
    if (e.key == "Escape") {
      onClose();
    }
  };

  return {
    open: onOpen,
    close: onClose,
    isOpen,
  };
}
