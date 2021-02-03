import React, { Component, createContext } from 'react';
import PropTypes from 'prop-types';
import { withTranslation } from 'react-i18next';
import { Table } from 'antd';
import EditableRow from '../../Molecules/TableUtils/EditableRow';
import EditableCell from '../../Molecules/TableUtils/EditableCell';
import CustomButton from '../../Atoms/CustomButton/CustomButton';
import './_style.scss';

const X_SCROLL_THRESHOLD = 6;
const COLUMN_WIDTH = 260;

const EditableContext = createContext();

class EditableTable extends Component {
  handleSave = row => {
    const { updateDataSource, dataSource } = this.props;
    const newData = [...dataSource];
    const index = newData.findIndex(item => row.key === item.key);
    const item = newData[index];
    newData.splice(index, 1, { ...item, ...row });
    updateDataSource(newData);
  };

  render() {
    const { t, deleteRow, dataSource, columns } = this.props;
    const components = {
      body: {
        row: props => <EditableRow {...props} EditableContext={EditableContext} />,
        cell: props => <EditableCell {...props} EditableContext={EditableContext} />
      }
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
          handleSave: this.handleSave,
          validations
        })
      };
    });

    const columnsWithActions = deleteRow
      ? realColumns.concat({
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
        })
      : realColumns;

    return (
      <Table
        components={components}
        rowClassName={() => 'editable-row'}
        bordered
        dataSource={dataSource}
        columns={columnsWithActions}
        pagination={false}
        locale={{ emptyText: ' ' }}
        scroll={
          realColumns.length > X_SCROLL_THRESHOLD ? { x: realColumns.length * COLUMN_WIDTH } : {}
        }
      />
    );
  }
}

export default withTranslation()(EditableTable);
