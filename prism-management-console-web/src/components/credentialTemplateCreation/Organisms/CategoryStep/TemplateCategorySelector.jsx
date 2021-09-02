import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { templateCategoryShape } from '../../../../helpers/propShapes';
import CategorySelector from './CategorySelector';
import TemplateName from './TemplateName';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';
import { PrismStoreContext } from '../../../../stores/domain/PrismStore';

const TemplateCategorySelectionStep = observer(() => {
  const { templateStore } = useContext(PrismStoreContext);
  const { templateCategories } = templateStore;

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
  templateCategories: PropTypes.arrayOf(PropTypes.shape({ templateCategoryShape })),
  mockCategoriesProps: PropTypes.shape({
    mockedCategories: templateCategoryShape,
    addMockedCategory: PropTypes.func.isRequired
  }).isRequired
};

export default TemplateCategorySelectionStep;
