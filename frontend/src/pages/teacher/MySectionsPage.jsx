import { Button, Table } from "react-bootstrap";
import { Link } from "react-router-dom";
import { getSectionsByPeriod } from "../../api/sections";
import PageHeader from "../../components/PageHeader";
import PeriodSelect from "../../components/PeriodSelect";
import QueryState from "../../components/QueryState";
import StatusBadge from "../../components/StatusBadge";
import useFetch from "../../hooks/useFetch";
import usePeriods from "../../hooks/usePeriods";

export default function MySectionsPage() {
  const periodsState = usePeriods();
  const { periods, selected, selectedId, setSelectedId } = periodsState;

  const { data, loading, error } = useFetch(
    () => (selectedId ? getSectionsByPeriod(selectedId) : Promise.resolve([])),
    [selectedId]
  );
  const sections = data ?? [];

  return (
    <>
      <PageHeader title="My sections" subtitle="Sections assigned to you" />
      <QueryState loading={periodsState.loading} error={periodsState.error}>
        {periods.length === 0 ? (
          <p className="text-muted">There are no academic periods yet.</p>
        ) : (
          <>
            <div className="d-flex align-items-center gap-3 mb-3">
              <PeriodSelect periods={periods} value={selectedId} onChange={setSelectedId} />
              {selected && <StatusBadge status={selected.status} />}
            </div>
            <QueryState loading={loading} error={error}>
              {sections.length === 0 ? (
                <p className="text-muted">You have no sections in this period.</p>
              ) : (
                <Table striped hover responsive size="sm" className="align-middle">
                  <thead>
                    <tr>
                      <th>Subject</th>
                      <th>Schedule</th>
                      <th>Capacity</th>
                      <th />
                    </tr>
                  </thead>
                  <tbody>
                    {sections.map((s) => (
                      <tr key={s.id}>
                        <td>
                          {s.subjectCode} - {s.subjectName}
                        </td>
                        <td>{s.blocks.join(", ")}</td>
                        <td>{s.capacity}</td>
                        <td className="text-end">
                          <Button as={Link} size="sm" to={`/teacher/sections/${s.id}/students`}>
                            Students and grades
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              )}
            </QueryState>
          </>
        )}
      </QueryState>
    </>
  );
}
