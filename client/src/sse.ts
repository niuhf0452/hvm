import {
  from,
  Observable,
  Subscriber,
  ReadableStreamLike,
  OperatorFunction,
  Subscription,
} from "rxjs";
import { switchMap } from "rxjs/operators";
import { fromFetch } from "rxjs/fetch";
import { createSignal, Accessor, onMount, onCleanup } from "solid-js";

export type SSEMessage = SSEIdMessage | SSEEventMessage | SSEDataMessage;
export type SSEIdMessage = {
  type: "id";
  id: string;
};
export type SSEEventMessage = {
  type: "event";
  value: string;
};
export type SSEDataMessage = {
  type: "data";
  value: string;
};

export function fetchStream(url: string, options?: RequestInit) {
  return fromFetch(url, options).pipe(
    switchMap((resp) => {
      if (resp.ok) {
        const body = resp.body?.pipeThrough(new TextDecoderStream());
        if (!body) {
          throw new Error("No body");
        }
        return from(body as ReadableStreamLike<string>);
      }
      return from(resp.text().then((text) => Promise.reject(new Error(text))));
    })
  );
}

export function parseSSE(): OperatorFunction<string, SSEMessage[]> {
  return (source) =>
    new Observable((subscriber) => {
      let buf = "";
      source.subscribe({
        next(chunk) {
          buf += chunk;
          buf = processSSE(buf, subscriber);
        },
        error(err) {
          subscriber.error(err);
        },
        complete() {
          subscriber.complete();
        },
      });
    });
}

function processSSE(data: string, sub: Subscriber<SSEMessage[]>): string {
  let offset = 0;
  while (offset < data.length) {
    const i = data.indexOf("\n\n", offset);
    if (i < 0) break;
    const arr: SSEMessage[] = [];
    data
      .substring(0, i)
      .split("\n")
      .forEach((line) => {
        if (line.startsWith("id:")) {
          arr.push({ type: "id", id: line.substring(3).trim() });
        } else if (line.startsWith("event:")) {
          arr.push({ type: "event", value: line.substring(6).trim() });
        } else if (line.startsWith("data:")) {
          arr.push({ type: "data", value: line.substring(5).trim() });
        }
      });
    if (arr.length > 0) {
      sub.next(arr);
    }
    offset = i + 2;
  }
  return offset > 0 ? data.substring(offset) : data;
}

export interface UseSSEOptions<T> {
  url: string;
  options?: RequestInit;
  refecthDelay?: number;
  defaultValue: T;
  onMessage: (items: T, messages: SSEMessage[]) => T;
  onError?: (err: unknown) => void;
  onComplete?: () => void;
}

export type SSEContext<T> = {
  value: Accessor<T>;
  refetch: () => void;
};

function delay(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export function useSSE<T>(options: UseSSEOptions<T>): SSEContext<T> {
  const [value, setValue] = createSignal<T>(options.defaultValue);
  let sub: Subscription | null = null;
  let lastFetchAt = 0;

  const refetch = async () => {
    const wait = lastFetchAt + (options.refecthDelay || 5000) - Date.now();
    if (wait > 0) {
      await delay(wait);
    }
    lastFetchAt = Date.now();
    sub?.unsubscribe();
    sub = fetchStream(options.url, options.options)
      .pipe(parseSSE())
      .subscribe({
        next(messages) {
          setValue((value) => options.onMessage(value, messages));
        },
        error(err) {
          (options.onError || refetch)(err);
        },
        complete() {
          (options.onComplete || refetch)();
        },
      });
  };

  onMount(refetch);

  onCleanup(() => sub?.unsubscribe());

  return { value, refetch };
}
