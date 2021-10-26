import React from 'react';
import PropTypes from 'prop-types';
import { getTemplatesColumns } from '../../../../helpers/tableDefinitions/templates';
import InfiniteScrollTable from '../../../common/Organisms/Tables/InfiniteScrollTable';
import { credentialTypeShape, templateCategoryShape } from '../../../../helpers/propShapes';
import './_style.scss';

const TemplatesTable = ({
  credentialTemplates,
  templateCategories,
  getMoreTemplates,
  isLoading,
  showTemplatePreview
}) => {
  const tableActions = {
    showTemplatePreview
  };

  const tableProps = {
    columns: getTemplatesColumns(templateCategories, tableActions),
    data: credentialTemplates,
    rowKey: 'id',
    hasMore: false,
    getMoreData: getMoreTemplates,
    loading: isLoading
  };

  return (
    <div className="TemplatesTableContainer InfiniteScrollTableContainer">
      <InfiniteScrollTable {...tableProps} />
    </div>
  );
};

TemplatesTable.propTypes = {
  credentialTemplates: PropTypes.arrayOf(credentialTypeShape).isRequired,
  templateCategories: PropTypes.arrayOf(templateCategoryShape).isRequired,
  showTemplatePreview: PropTypes.func.isRequired,
  isLoading: PropTypes.bool.isRequired,
  getMoreTemplates: PropTypes.func.isRequired
};

export default TemplatesTable;
