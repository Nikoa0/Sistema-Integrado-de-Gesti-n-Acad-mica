import ErrorAlert from "./ErrorAlert";
import LoadingSpinner from "./LoadingSpinner";

// Shows a spinner or an error instead of its children. Children are evaluated eagerly,
// so always guard data with `data ?? []` before using it inside them.
export default function QueryState({ loading, error, children }) {
  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorAlert message={error} />;
  return children;
}
