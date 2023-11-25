import toast from "solid-toast";

let fetchCounter = 0;
let toastId = "";

export default function fetch(input: RequestInfo | URL, init?: RequestInit) {
  const result = window.fetch(input, init);
  if (fetchCounter++ === 0) {
    toastId = toast.loading("Loading...", {
      position: "top-center",
    });
  }
  result.finally(() => {
    if (--fetchCounter === 0) {
      toast.dismiss(toastId);
    }
  });
  return result;
}
