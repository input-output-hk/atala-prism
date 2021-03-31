export const getCheckedAndIndeterminateProps = (rows, selectedRows) => ({
  checked: rows.length && selectedRows.length === rows.length,
  indeterminate: selectedRows.length && selectedRows.length !== rows.length
});

export const handleSelectAll = async ({
  ev,
  setSelected,
  entities,
  hasMore,
  idKey,
  fetchAll,
  setLoading
}) => {
  setLoading(true);
  const { checked } = ev.target;
  setSelected(checked ? entities.map(e => e[idKey]) : []);
  if (checked && hasMore) {
    const fetchedAndFilteredContacts = await fetchAll();
    setSelected(fetchedAndFilteredContacts.map(e => e[idKey]));
  }
  setLoading(false);
};
