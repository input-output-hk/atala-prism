import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { SELECT_CREDENTIAL_TYPE_STEP } from '../../../../helpers/constants';
import TemplatesAdditionalFilters from '../../../credentialTemplates/Molecules/Filters/TemplatesAdditionalFilters';
import SearchBar from '../../../common/Atoms/SearchBar/SearchBar';
import WizardTitle from '../../../common/Atoms/WizardTitle/WizardTitle';
import { templateCategoryShape } from '../../../../helpers/propShapes';

const NewCredentialStepHeader = ({ currentStep, filterSortingProps, templateCategories }) => {
  const { t } = useTranslation();

  const { nameFilter, setFilterValue } = filterSortingProps;

  return (
    <div key="step-header" className="StepHeader">
      <WizardTitle
        title={t(`newCredential.title.step${currentStep + 1}`)}
        subtitle={t(`newCredential.subtitle.step${currentStep + 1}`)}
      />
      {currentStep === SELECT_CREDENTIAL_TYPE_STEP && (
        <div className="ActionsHeader EmbeddedTempalteFilters flex">
          <SearchBar
            searchText={nameFilter}
            setSearchText={value => setFilterValue('nameFilter', value)}
            placeholder={t('templates.actions.searchPlaceholder')}
          />
          <TemplatesAdditionalFilters
            templateCategories={templateCategories}
            filterSortingProps={filterSortingProps}
          />
        </div>
      )}
    </div>
  );
};

NewCredentialStepHeader.propTypes = {
  currentStep: PropTypes.number.isRequired,
  filterSortingProps: PropTypes.shape({
    nameFilter: PropTypes.string,
    setFilterValue: PropTypes.func
  }).isRequired,
  templateCategories: PropTypes.arrayOf(templateCategoryShape).isRequired
};

export default NewCredentialStepHeader;
