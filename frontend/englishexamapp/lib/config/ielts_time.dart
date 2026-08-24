class IeltsTime {
  static const listening = Duration(minutes: 30);
  static const reading = Duration(minutes: 60);
  static const writing = Duration(minutes: 60);

  static Duration forSkill(String skillType) {
    switch (skillType) {
      case 'LISTENING':
        return listening;
      case 'READING':
        return reading;
      case 'WRITING':
        return writing;
      default:
        return Duration.zero;
    }
  }
}
