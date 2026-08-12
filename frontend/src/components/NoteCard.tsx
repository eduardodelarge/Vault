import { Link } from 'react-router-dom';
import type { NoteSummary } from '../types/note';

export function NoteCard({ note }: { note: NoteSummary }) {
  const updated = new Date(note.updatedAt).toLocaleString('pt-BR');

  return (
    <Link to={`/notes/${note.id}`} className="note-card">
      <h3>{note.title}</h3>
      <span className="note-card-date">Atualizado em {updated}</span>
    </Link>
  );
}
