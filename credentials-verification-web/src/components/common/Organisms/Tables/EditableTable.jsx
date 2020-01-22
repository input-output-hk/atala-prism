import React, { createContext } from 'react';
import PropTypes from 'prop-types';
import { Table } from 'antd';
import EditableRow from '../../Molecules/TableUtils/EditableRow';
import EditableCell from '../../Molecules/TableUtils/EditableCell';

const EdtiableTable = ({ dataSource, columns }) => {
  const EditableContext = createContext();

  const components = {
    body: {
      row: props => <EditableRow {...props} EditableContext={EditableContext} />,
      cell: props => <EditableCell {...props} EditableContext={EditableContext} />
    }
  };

  return (
    <div className="EditableTable">
      <Table
        components={components}
        bordered
        dataSource={dataSource}
        columns={columns}
        pagination={false}
        locale={{ emptyText: ' ' }}
      />
    </div>
  );
};

EdtiableTable.defaultProps = {
  dataSource: []
};

EdtiableTable.propTypes = {
  dataSource: PropTypes.arrayOf(PropTypes.shape()),
  columns: PropTypes.arrayOf(PropTypes.shape()).isRequired
};

export default EdtiableTable;
