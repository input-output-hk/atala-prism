import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import EdtiableTable from '../../../common/Organisms/Tables/EditableTable';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

const getColumns = (length, t, handleDelete) => [
  {
    title: t('individualCreation.table.fullName'),
    dataIndex: 'fullName',
    width: '30%',
    editable: true
  },
  {
    title: t('individualCreation.table.email'),
    dataIndex: 'email',
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
        buttonText={t('individualCreation.table.delete')}
      />
    )
  }
];

const IndividualCreationTable = ({ individuals, deleteIndividual, editIndividual }) => {
  const { t } = useTranslation();

  const columns = getColumns(individuals.length, t, deleteIndividual);

  const realColumns = columns.map(col => {
    const { editable, dataIndex, title } = col;
    if (!editable) {
      return col;
    }

    return {
      ...col,
      onCell: record => ({
        record,
        editable,
        dataIndex,
        title,
        handleSave: editIndividual
      })
    };
  });

  const tableProps = {
    dataSource: individuals,
    columns: realColumns
  };

  return <EdtiableTable {...tableProps} />;
};

IndividualCreationTable.defaultProps = {
  individuals: []
};

IndividualCreationTable.propTypes = {
  individuals: PropTypes.arrayOf(PropTypes.shape),
  deleteIndividual: PropTypes.func.isRequired,
  editIndividual: PropTypes.func.isRequired
};

export default IndividualCreationTable;
