import { httpClient } from './httpClient';
import type { Note, NotePage } from '../types/note';

export function searchNotes(q: string, page = 0, size = 20) {
  return httpClient
    .get<NotePage>('/notes', { params: { q: q || undefined, page, size } })
    .then((res) => res.data);
}

export function getNote(id: string) {
  return httpClient.get<Note>(`/notes/${id}`).then((res) => res.data);
}

export function createNote(title: string, content: string) {
  return httpClient.post<Note>('/notes', { title, content }).then((res) => res.data);
}

export function updateNote(id: string, title: string, content: string) {
  return httpClient.put<Note>(`/notes/${id}`, { title, content }).then((res) => res.data);
}

export function deleteNote(id: string) {
  return httpClient.delete<void>(`/notes/${id}`);
}
