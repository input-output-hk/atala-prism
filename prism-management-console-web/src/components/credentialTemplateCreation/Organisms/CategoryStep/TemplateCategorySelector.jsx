import React from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { templateCategoryShape } from '../../../../helpers/propShapes';
import CategorySelector from './CategorySelector';
import TemplateName from './TemplateName';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';
import { useTemplateStore } from '../../../../hooks/useTemplateStore';

const TemplateCategorySelectionStep = observer(() => {
  const { templateCategories } = useTemplateStore();

  if (!templateCategories.length) return <SimpleLoading size="md" />;
  return (
    <div className="TypeSelectionWrapper">
      <div className="TypeSelectionContainer">
        <div className="TypeSelection">
          <div className="verticalFlex flexStart fullWidth">
            <TemplateName />
            <CategorySelector templateCategories={templateCategories} />
          </div>
        </div>
      </div>
    </div>
  );
});

TemplateCategorySelectionStep.defaultProps = {
  templateCategories: []
};

TemplateCategorySelectionStep.propTypes = {
  templateCategories: PropTypes.arrayOf(PropTypes.shape({ templateCategoryShape }))
};

export default TemplateCategorySelectionStep;
