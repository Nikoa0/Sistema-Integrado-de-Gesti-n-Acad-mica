import { useState } from "react";
import { getPeriods } from "../api/academicPeriods";
import useFetch from "./useFetch";

// Loads the academic periods and keeps the selected one (defaults to the newest OPEN period).
export default function usePeriods() {
  const { data, loading, error } = useFetch(() => getPeriods(), []);
  const [chosenId, setChosenId] = useState(null);

  const periods = data ?? [];
  const fallback = periods.find((p) => p.status === "OPEN") ?? periods[0];
  const selectedId = chosenId ?? fallback?.id ?? null;
  const selected = periods.find((p) => p.id === selectedId) ?? null;

  return { periods, selected, selectedId, setSelectedId: setChosenId, loading, error };
}
