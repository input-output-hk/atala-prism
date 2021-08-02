import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { withTranslation } from 'react-i18next';
import DynamicForm from '../../../dynamicForm/DynamicForm';
import { skeletonShape } from '../../../../helpers/propShapes';
import './_style.scss';
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
    const { columns, skeleton, useCase, initialValues } = this.props;

    return (
      <DynamicForm
        skeleton={skeleton}
        columns={columns}
        initialValues={initialValues}
        useCase={useCase}
      />
    );
  }
}

EditableTable.defaultProps = {
  preExistingEntries: [],
  initialValues: []
};

EditableTable.propTypes = {
  updateDataSource: PropTypes.func.isRequired,
  dataSource: PropTypes.shape({}).isRequired,
  useCase: PropTypes.string.isRequired,
  columns: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
  skeleton: skeletonShape.isRequired,
  initialValues: PropTypes.arrayOf(PropTypes.shape({}))
};

export default withTranslation()(EditableTable);
