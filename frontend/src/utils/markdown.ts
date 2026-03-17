import type { TaskEstimate } from '@/components/ResultsTable';

function escapeMarkdown(text: string): string {
  return text.replace(/\|/g, '\\|');
}

function getTaskSp(task: TaskEstimate): number {
  if (task.finalEstimate != null) {
    const n = Number(task.finalEstimate);
    return isNaN(n) ? 0 : n;
  }
  return task.median ?? 0;
}

function countConsensus(tasks: TaskEstimate[]): number {
  return tasks.filter((t) => {
    const nums = Object.values(t.estimates).filter((v): v is number => typeof v === 'number');
    return nums.length > 0 && new Set(nums).size === 1;
  }).length;
}

export function formatResultsAsMarkdown(
  roomTitle: string,
  tasks: TaskEstimate[],
  participants: string[],
): string {
  const lines: string[] = [`# Results: ${roomTitle}`, ''];

  for (const task of tasks) {
    lines.push(`## ${task.taskTitle}`);

    for (const name of participants) {
      const sp = task.estimates[name];
      const comment = task.comments[name];

      if (sp == null) {
        lines.push(`- ${name}: -`);
      } else if (comment) {
        lines.push(`- ${name}: **${sp}** - ${escapeMarkdown(comment)}`);
      } else {
        lines.push(`- ${name}: **${sp}**`);
      }
    }

    lines.push('');

    const parts: string[] = [];
    if (task.average != null) parts.push(`Avg: ${task.average.toFixed(1)}`);
    if (task.median != null) parts.push(`Med: ${task.median}`);
    if (task.finalEstimate != null) parts.push(`Final: ${task.finalEstimate}`);

    if (parts.length > 0) {
      lines.push(`> ${parts.join(' | ')}`);
      lines.push('');
    }
  }

  if (tasks.length > 0) {
    const totalSp = tasks.reduce((sum, t) => sum + getTaskSp(t), 0);
    const consensus = countConsensus(tasks);
    lines.push('---');
    lines.push(`**Summary:** ${tasks.length} tasks | ${totalSp} SP | Consensus: ${consensus}/${tasks.length}`);
  }

  return lines.join('\n').trimEnd() + '\n';
}
