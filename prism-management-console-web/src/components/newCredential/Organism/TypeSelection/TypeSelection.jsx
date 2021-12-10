import React from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { useTranslation } from 'react-i18next';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';
import { CONFIRMED } from '../../../../helpers/constants';
import { useTemplatesByCategoryStore } from '../../../../hooks/useTemplatesPageStore';
import TemplatesList from './TemplatesList';
import CreateTemplateButton from '../../../credentialTemplates/Atoms/Buttons/CreateTemplateButton';
import EmptyComponent from '../../../common/Atoms/EmptyComponent/EmptyComponent';
import noTemplatesPicture from '../../../../images/noTemplates.svg';
import { useSession } from '../../../../hooks/useSession';

import './_style.scss';

const TypeSelection = observer(({ selectedType, onTypeSelection }) => {
  const { t } = useTranslation();
  const { accountStatus } = useSession();
  const { filteredTemplates, isLoading, filterSortingProps } = useTemplatesByCategoryStore();
  const { hasFiltersApplied } = filterSortingProps;

  const noTemplates = !filteredTemplates?.length;

  const emptyProps = {
    photoSrc: noTemplatesPicture,
    model: t('templates.title'),
    isFilter: hasFiltersApplied,
    button: noTemplates && accountStatus === CONFIRMED ? <CreateTemplateButton /> : null
  };

  if (isLoading) return <SimpleLoading size="md" />;
  if (noTemplates) return <EmptyComponent {...emptyProps} />;

  return (
    <div className="TypeSelectionWrapper">
      <div className="TypeSelectionContainer">
        <div className="TypeSelection">
          <TemplatesList selectedType={selectedType} onTypeSelection={onTypeSelection} />
        </div>
      </div>
    </div>
  );
});

TypeSelection.defaultProps = {
  selectedType: ''
};

TypeSelection.propTypes = {
  selectedType: PropTypes.string,
  onTypeSelection: PropTypes.func.isRequired
};

export default TypeSelection;
