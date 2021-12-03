import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { useTemplateCreationStore } from '../../../../hooks/useTemplatesPageStore';
import IconSelector from './IconSelector';
import { svgPathToEncodedBase64 } from '../../../../helpers/genericHelpers';
import './_style.scss';

const i18nPrefix = 'credentialTemplateCreation';

const IconPicker = observer(({ close }) => {
  const { t } = useTranslation();
  const { templateSketch, setSketchState } = useTemplateCreationStore();

  const defaultIcon = { index: 0, src: templateSketch.icon, isCustomIcon: false };
  const [selectedIcon, setSelectedIcon] = useState(defaultIcon);

  const handleSaveIcon = async () => {
    const iconData = selectedIcon.isCustomIcon
      ? selectedIcon.file.thumbUrl
      : await svgPathToEncodedBase64(selectedIcon.src);
    setSketchState({ icon: iconData });
    close();
  };

  return (
    <>
      <IconSelector selectedIcon={selectedIcon} setSelectedIcon={setSelectedIcon} />
      <div className="buttonSection">
        <CustomButton
          buttonText={t(`${i18nPrefix}.iconPicker.save`)}
          buttonProps={{
            className: 'theme-secondary',
            onClick: handleSaveIcon
          }}
        />
      </div>
    </>
  );
});

IconPicker.propTypes = {
  close: PropTypes.func.isRequired
};

export default IconPicker;
