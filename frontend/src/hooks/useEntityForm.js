import { useState } from "react";
import { getErrorMessage } from "../utils/errors";

// State of a create/edit modal.
//   empty:  initial values of the form
//   save:   (form, editingItem | null) => Promise
//   onDone: called after a successful save (usually the list reload)
export default function useEntityForm(empty, save, onDone) {
  const [show, setShow] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(empty);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const openCreate = () => {
    setEditing(null);
    setForm(empty);
    setError(null);
    setShow(true);
  };

  const openEdit = (item, values) => {
    setEditing(item);
    setForm(values);
    setError(null);
    setShow(true);
  };

  const close = () => setShow(false);

  // <Form.Control onChange={editor.setField("name")} />
  const setField = (name) => (e) => {
    const value = e.target.type === "checkbox" ? e.target.checked : e.target.value;
    setForm((f) => ({ ...f, [name]: value }));
  };

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await save(form, editing);
      setShow(false);
      onDone();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return { show, editing, form, setForm, setField, saving, error, openCreate, openEdit, close, submit };
}
