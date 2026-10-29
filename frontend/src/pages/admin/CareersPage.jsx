import { useState } from "react";
import { Button, Form, Table } from "react-bootstrap";
import { createCareer, deleteCareer, getCareers, updateCareer } from "../../api/careers";
import ConfirmModal from "../../components/ConfirmModal";
import ErrorAlert from "../../components/ErrorAlert";
import Field from "../../components/Field";
import FormModal from "../../components/FormModal";
import PageHeader from "../../components/PageHeader";
import QueryState from "../../components/QueryState";
import StatusBadge from "../../components/StatusBadge";
import { CAREER_STATUSES } from "../../constants/enums";
import useAction from "../../hooks/useAction";
import useEntityForm from "../../hooks/useEntityForm";
import useFetch from "../../hooks/useFetch";

const EMPTY = { code: "", name: "", description: "", status: "ACTIVE" };

export default function CareersPage() {
  const { data, loading, error, reload } = useFetch(() => getCareers(), []);
  const careers = data ?? [];
  const action = useAction();
  const [toDelete, setToDelete] = useState(null);

  const editor = useEntityForm(
    EMPTY,
    (form, editing) => (editing ? updateCareer(editing.id, form) : createCareer(form)),
    reload
  );

  const confirmDelete = async () => {
    await action.run(() => deleteCareer(toDelete.id));
    setToDelete(null);
    reload();
  };

  return (
    <>
      <PageHeader
        title="Careers"
        subtitle="Manage the careers offered by the institute"
        actions={<Button onClick={editor.openCreate}>New career</Button>}
      />
      <ErrorAlert message={action.error} onClose={action.clearError} />

      <QueryState loading={loading} error={error}>
        {careers.length === 0 ? (
          <p className="text-muted">There are no careers yet. Create the first one.</p>
        ) : (
          <Table striped hover responsive size="sm" className="align-middle">
            <thead>
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>Description</th>
                <th>Semesters</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {careers.map((c) => (
                <tr key={c.id}>
                  <td>{c.code}</td>
                  <td>{c.name}</td>
                  <td>{c.description}</td>
                  <td>{c.durationSemesters}</td>
                  <td>
                    <StatusBadge status={c.status} />
                  </td>
                  <td className="text-end text-nowrap">
                    <Button
                      size="sm"
                      variant="outline-primary"
                      className="me-2"
                      onClick={() =>
                        editor.openEdit(c, {
                          code: c.code,
                          name: c.name,
                          description: c.description,
                          status: c.status,
                        })
                      }
                    >
                      Edit
                    </Button>
                    <Button size="sm" variant="outline-danger" onClick={() => setToDelete(c)}>
                      Delete
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </QueryState>

      <FormModal
        show={editor.show}
        title={editor.editing ? "Edit career" : "New career"}
        onHide={editor.close}
        onSubmit={editor.submit}
        saving={editor.saving}
        error={editor.error}
      >
        <Field label="Code">
          <Form.Control required maxLength={20} value={editor.form.code} onChange={editor.setField("code")} />
        </Field>
        <Field label="Name">
          <Form.Control required maxLength={150} value={editor.form.name} onChange={editor.setField("name")} />
        </Field>
        <Field label="Description">
          <Form.Control
            as="textarea"
            rows={3}
            required
            maxLength={1000}
            value={editor.form.description}
            onChange={editor.setField("description")}
          />
        </Field>
        <Field label="Status" hint="An INACTIVE career cannot receive new students.">
          <Form.Select value={editor.form.status} onChange={editor.setField("status")}>
            {CAREER_STATUSES.map((s) => (
              <option key={s}>{s}</option>
            ))}
          </Form.Select>
        </Field>
      </FormModal>

      <ConfirmModal
        show={Boolean(toDelete)}
        title="Delete career"
        message={`Delete the career "${toDelete?.name}"? It can only be deleted if it has no study plans or students.`}
        confirmLabel="Delete"
        onConfirm={confirmDelete}
        onCancel={() => setToDelete(null)}
      />
    </>
  );
}
