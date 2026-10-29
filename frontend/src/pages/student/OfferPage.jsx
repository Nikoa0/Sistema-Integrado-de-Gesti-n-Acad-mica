import { Alert, Table } from "react-bootstrap";
import { getOffer } from "../../api/enrollments";
import { useAuth } from "../../auth/useAuth";
import PageHeader from "../../components/PageHeader";
import PeriodSelect from "../../components/PeriodSelect";
import QueryState from "../../components/QueryState";
import StatusBadge from "../../components/StatusBadge";
import useFetch from "../../hooks/useFetch";
import usePeriods from "../../hooks/usePeriods";

export default function OfferPage() {
  const { studentId } = useAuth();
  const periodsState = usePeriods();
  const { periods, selected, selectedId, setSelectedId } = periodsState;

  const { data, loading, error } = useFetch(
    () => (studentId && selectedId ? getOffer(studentId, selectedId) : Promise.resolve([])),
    [studentId, selectedId]
  );
  const offer = (data ?? []).slice().sort((a, b) => a.subjectCode.localeCompare(b.subjectCode));

  return (
    <>
      <PageHeader title="Academic offer" subtitle="Sections offered in the period for your study plan" />
      {!studentId && <Alert variant="warning">No student record is linked to your user.</Alert>}
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
              {offer.length === 0 ? (
                <p className="text-muted">No sections are offered for your study plan in this period.</p>
              ) : (
                <Table striped hover responsive size="sm" className="align-middle">
                  <thead>
                    <tr>
                      <th>Subject</th>
                      <th>Teacher</th>
                      <th>Schedule</th>
                      <th>Capacity</th>
                    </tr>
                  </thead>
                  <tbody>
                    {offer.map((s) => (
                      <tr key={s.id}>
                        <td>
                          {s.subjectCode} - {s.subjectName}
                        </td>
                        <td>{s.teacherName}</td>
                        <td>{s.blocks.join(", ")}</td>
                        <td>{s.capacity}</td>
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
