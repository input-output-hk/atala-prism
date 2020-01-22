import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import EdtiableTable from '../../../common/Organisms/Tables/EditableTable';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

const getColumns = (length, t, handleDelete) => [
  {
    title: t('studentCreation.table.fullName'),
    dataIndex: 'fullName',
    width: '30%',
    editable: true
  },
  {
    title: t('studentCreation.table.email'),
    dataIndex: 'email',
    editable: true
  },
  {
    title: t('studentCreation.table.studentId'),
    dataIndex: 'universityAssignedId',
    editable: true
  },
  {
    title: t('studentCreation.table.admissionDate'),
    dataIndex: 'admissionDate',
    type: 'date',
    editable: true
  },
  {
    dataIndex: 'action',
    render: (_text, { key }) => (
      // TODO add tooltip
      <CustomButton
        buttonProps={{
          onClick: () => handleDelete(key),
          className: 'theme-link',
          disabled: length <= 1
        }}
        buttonText={t('studentCreation.table.delete')}
      />
    )
  }
];

const StudentCreationTable = ({ students, deleteStudent, editStudent }) => {
  const { t } = useTranslation();

  const columns = getColumns(students.length, t, deleteStudent);

  const realColumns = columns.map(col => {
    const { editable, dataIndex, title, type } = col;
    if (!editable) {
      return col;
    }

    return {
      ...col,
      onCell: record => ({
        record,
        editable,
        type,
        dataIndex,
        title,
        handleSave: editStudent
      })
    };
  });

  const tableProps = {
    dataSource: students,
    columns: realColumns
  };

  return <EdtiableTable {...tableProps} />;
};

StudentCreationTable.defaultProps = {
  students: []
};

StudentCreationTable.propTypes = {
  students: PropTypes.arrayOf(PropTypes.shape),
  deleteStudent: PropTypes.func.isRequired,
  editStudent: PropTypes.func.isRequired
};

export default StudentCreationTable;
