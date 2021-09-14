import React from 'react';
import PropTypes from 'prop-types';
import { templateCategoryShape } from '../../../../helpers/propShapes';
import CategorySelector from './CategorySelector';
import TemplateName from './TemplateName';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';
import UploadTemplateIcon from '../../Molecules/UploadTemplateIcon/UploadTemplateIcon';

const TemplateCategorySelectionStep = ({ templateCategories, mockCategoriesProps }) => {
  if (!templateCategories.length) return <SimpleLoading size="md" />;
  return (
    <div className="templateCreation">
      <UploadTemplateIcon />
      <div className="categoryAssign">
        <TemplateName />
        <CategorySelector
          templateCategories={templateCategories}
          mockCategoriesProps={mockCategoriesProps}
        />
      </div>
    </div>
  );
};

TemplateCategorySelectionStep.defaultProps = {
  templateCategories: []
};

TemplateCategorySelectionStep.propTypes = {
  templateCategories: PropTypes.arrayOf(PropTypes.shape({ templateCategoryShape })),
  mockCategoriesProps: PropTypes.shape({
    mockedCategories: templateCategoryShape,
    addMockedCategory: PropTypes.func.isRequired
  }).isRequired
};

export default TemplateCategorySelectionStep;
