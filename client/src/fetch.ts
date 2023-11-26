import toast from "solid-toast";

let fetchCounter = 0;
let toastId = "";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function fetch(input: any, init: any): Promise<any> {
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

const typedFetch = fetch as typeof window.fetch;

export default typedFetch;
