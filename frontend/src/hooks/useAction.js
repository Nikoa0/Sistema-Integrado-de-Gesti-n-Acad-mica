import { useState } from "react";
import { getErrorMessage } from "../utils/errors";

// Runs an async action (delete, close, enroll...) and keeps its error message.
// run() resolves with the action result, or undefined if it failed.
export default function useAction() {
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const run = async (fn) => {
    setBusy(true);
    setError(null);
    try {
      return await fn();
    } catch (err) {
      setError(getErrorMessage(err));
      return undefined;
    } finally {
      setBusy(false);
    }
  };

  return { run, error, busy, clearError: () => setError(null) };
}
