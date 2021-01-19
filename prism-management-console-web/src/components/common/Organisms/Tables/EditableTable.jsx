import React, { createContext } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Table } from 'antd';
import EditableRow from '../../Molecules/TableUtils/EditableRow';
import EditableCell from '../../Molecules/TableUtils/EditableCell';
import CustomButton from '../../Atoms/CustomButton/CustomButton';
import './_style.scss';

const EditableTable = ({ dataSource, columns, deleteRow, updateDataSource }) => {
  const EditableContext = createContext();
  const { t } = useTranslation();

  const components = {
    body: {
      row: props => <EditableRow {...props} EditableContext={EditableContext} />,
      cell: props => <EditableCell {...props} EditableContext={EditableContext} />
    }
  };

  const handleSave = row => {
    const newData = [...dataSource];
    const index = newData.findIndex(item => row.key === item.key);
    const item = newData[index];
    newData.splice(index, 1, {
      ...item,
      ...row
    });
    updateDataSource(newData);
  };

  const realColumns = columns.map(col => {
    const { editable, dataIndex, title, type, validations } = col;
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
        handleSave,
        validations
      })
    };
  });

  const columnsWithActions = realColumns.concat({
    title: 'Actions',
    dataIndex: 'action',
    render: (text, record) => (
      <CustomButton
        buttonProps={{
          onClick: () => deleteRow(record.key),
          className: 'theme-link'
        }}
        buttonText={t('actions.delete')}
      />
    )
  });

  return (
    <div className="EditableTable">
      <Table
        components={components}
        bordered
        dataSource={dataSource}
        columns={columnsWithActions}
        pagination={false}
        locale={{ emptyText: ' ' }}
      />
    </div>
  );
};

EditableTable.defaultProps = {
  dataSource: []
};

EditableTable.propTypes = {
  dataSource: PropTypes.arrayOf(PropTypes.shape()),
  columns: PropTypes.arrayOf(PropTypes.shape()).isRequired,
  deleteRow: PropTypes.func.isRequired,
  updateDataSource: PropTypes.func.isRequired
};

export default EditableTable;
