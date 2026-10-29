import { useCallback, useEffect, useState } from "react";
import { getErrorMessage } from "../utils/errors";

// Loads data from an API function and exposes { data, loading, error, reload }.
// Usage: const { data, loading, error, reload } = useFetch(() => getCareers(), []);
export default function useFetch(fetcher, deps = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    let cancelled = false;
    fetcher()
      .then((result) => {
        if (!cancelled) {
          setData(result);
          setError(null);
        }
      })
      .catch((err) => {
        if (!cancelled) setError(getErrorMessage(err));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, version]);

  const reload = useCallback(() => {
    setLoading(true);
    setVersion((v) => v + 1);
  }, []);

  return { data, loading, error, reload };
}
